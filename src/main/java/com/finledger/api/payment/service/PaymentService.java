package com.finledger.api.payment.service;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.common.NotFoundException;
import com.finledger.api.expense.domain.ExpenseSplit;
import com.finledger.api.expense.service.ExpenseService;
import com.finledger.api.payment.api.PaymentDtos.*;
import com.finledger.api.payment.domain.Payment;
import com.finledger.api.payment.event.SharePaidEvent;
import com.finledger.api.payment.gateway.GatewayEvent;
import com.finledger.api.payment.gateway.PaymentGateway;
import com.finledger.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment use cases. Depends on the PaymentGateway interface, never on Razorpay directly.
 *
 * A payment can be recorded by two paths, which both end in {@link #apply}:
 *  - the webhook (primary, pushed by Razorpay), and
 *  - reconciliation in verify() (fallback, pulled from Razorpay when the webhook is late or lost).
 * apply() runs on a locked row and skips anything already CAPTURED, so whichever path comes second is a no-op.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ExpenseService expenseService;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;

    /**
     * Creates (or reuses) a Razorpay order for the caller's unpaid share.
     * The expense row stays locked for the whole transaction, so a double tap gets the same order.
     */
    @Transactional
    public PayResponse pay(Long userId, Long expenseId) {
        ExpenseSplit share = expenseService.lockPayableShare(userId, expenseId);

        Payment payment = payments
                .findFirstByExpenseIdAndUserIdAndStatus(expenseId, userId, Payment.Status.CREATED)
                .filter(p -> p.getAmount().compareTo(share.getAmountOwed()) == 0)
                .orElseGet(() -> {
                    String receipt = "exp" + expenseId + "-u" + userId + "-" + System.currentTimeMillis();
                    String orderId = gateway.createOrder(share.getAmountOwed(), receipt);
                    return payments.save(Payment.create(expenseId, userId, orderId, share.getAmountOwed()));
                });

        return new PayResponse(payment.getRazorpayOrderId(), payment.amountPaise(),
                payment.getAmount().setScale(2).toPlainString(),
                "INR", gateway.publicKey(), "FinLedger split #" + expenseId);
    }

    /** Primary path: Razorpay pushed an event to /webhooks/razorpay. Idempotent. */
    @Transactional
    public void handleWebhook(GatewayEvent ev) {
        if (ev.type() == GatewayEvent.Type.IGNORED) return;

        Payment p = payments.lockByOrderId(ev.orderId()).orElse(null);
        if (p == null) { log.warn("Webhook for unknown order {}", ev.orderId()); return; }
        apply(p, ev, "webhook");
    }

    /**
     * App calls this right after Checkout.
     * 1. Checks the Checkout signature (proves Razorpay issued this payment for this order).
     * 2. Fallback / reconciliation: if the webhook hasn't arrived yet, asks Razorpay for the payment's
     *    real status and records it through the same idempotent path as the webhook.
     */
    @Transactional
    public PaymentStatusDto verify(Long userId, VerifyRequest req) {
        Payment p = payments.lockByOrderId(req.orderId())                 // lock first: no race with a webhook
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (!gateway.isPaymentSignatureValid(req.orderId(), req.paymentId(), req.signature()))
            throw new BusinessRuleException("Payment signature is invalid");

        if (!p.isCaptured()) {
            GatewayEvent ev = gateway.fetchPayment(req.paymentId());
            if (ev.type() != GatewayEvent.Type.IGNORED && !req.orderId().equals(ev.orderId()))
                throw new BusinessRuleException("Payment does not belong to this order");
            apply(p, ev, "reconciliation");
        }
        return toStatus(p);
    }

    /** App polls this until status becomes CAPTURED. */
    @Transactional(readOnly = true)
    public PaymentStatusDto status(Long userId, String orderId) {
        Payment p = payments.findByRazorpayOrderId(orderId)
                .filter(x -> x.getUserId().equals(userId))                // can't look at someone else's payment
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return toStatus(p);
    }

    /**
     * The single place a gateway event changes a payment. Caller must hold the row lock.
     * Already CAPTURED -> ignored (idempotent). FAILED -> mark failed. CAPTURED -> check amount, capture, notify.
     */
    private void apply(Payment p, GatewayEvent ev, String source) {
        if (ev.type() == GatewayEvent.Type.IGNORED) return;
        if (p.isCaptured()) { log.info("{}: {} already captured, ignored", source, p.getRazorpayOrderId()); return; }

        if (ev.type() == GatewayEvent.Type.FAILED) {
            if (p.isCreated()) p.fail();
            return;
        }
        if (!p.hasAmountPaise(ev.amountPaise())) {
            log.error("{}: amount mismatch for {}: expected {} paise, got {}",
                    source, p.getRazorpayOrderId(), p.amountPaise(), ev.amountPaise());
            return;
        }
        p.capture(ev.paymentId());
        log.info("{}: captured {} ({})", source, p.getRazorpayOrderId(), ev.paymentId());
        events.publishEvent(new SharePaidEvent(p.getExpenseId(), p.getUserId(), ev.paymentId()));
    }

    private static PaymentStatusDto toStatus(Payment p) {
        return new PaymentStatusDto(p.getRazorpayOrderId(), p.getExpenseId(),
                p.getAmount().setScale(2).toPlainString(), p.getStatus().name());
    }
}
