package com.finledger.api.payment.service;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.common.NotFoundException;
import com.finledger.api.expense.domain.ExpenseSplit;
import com.finledger.api.expense.service.ExpenseService;
import com.finledger.api.payment.api.PaymentDtos.*;
import com.finledger.api.payment.domain.Payment;
import com.finledger.api.payment.event.SharePaidEvent;
import com.finledger.api.payment.gateway.*;
import com.finledger.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Payment use cases. Depends on the PaymentGateway interface, never on Razorpay directly. */
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

        return new PayResponse(payment.getRazorpayOrderId(),
                RazorpayGateway.toPaise(payment.getAmount()),
                payment.getAmount().setScale(2).toPlainString(),
                "INR", gateway.publicKey(), "FinLedger split #" + expenseId);
    }

    /**
     * Webhook handler. Idempotent: Razorpay may deliver the same event several times,
     * so anything already CAPTURED is skipped. The payment row is locked to serialize duplicates.
     */
    @Transactional
    public void handleWebhook(GatewayEvent ev) {
        if (ev.type() == GatewayEvent.Type.IGNORED) return;

        Payment p = payments.lockByOrderId(ev.orderId()).orElse(null);
        if (p == null) { log.warn("Webhook for unknown order {}", ev.orderId()); return; }
        if (p.isCaptured()) { log.info("Duplicate webhook for {}, ignored", ev.orderId()); return; }

        if (ev.type() == GatewayEvent.Type.FAILED) {
            if (p.isCreated()) p.fail();
            return;
        }
        if (!p.hasAmountPaise(ev.amountPaise())) {
            log.error("Amount mismatch for {}: expected {}, got {} paise", ev.orderId(), p.getAmount(), ev.amountPaise());
            return;
        }
        p.capture(ev.paymentId());
        events.publishEvent(new SharePaidEvent(p.getExpenseId(), p.getUserId(), ev.paymentId()));
    }

    /** App calls this right after Checkout. Confirms the signature only; the webhook is what marks the share PAID. */
    @Transactional(readOnly = true)
    public PaymentStatusDto verify(Long userId, VerifyRequest req) {
        Payment p = ownPayment(userId, req.orderId());
        if (!gateway.isPaymentSignatureValid(req.orderId(), req.paymentId(), req.signature()))
            throw new BusinessRuleException("Payment signature is invalid");
        return toStatus(p);
    }

    /** App polls this until status becomes CAPTURED. */
    @Transactional(readOnly = true)
    public PaymentStatusDto status(Long userId, String orderId) {
        return toStatus(ownPayment(userId, orderId));
    }

    private Payment ownPayment(Long userId, String orderId) {
        return payments.findByRazorpayOrderId(orderId)
                .filter(p -> p.getUserId().equals(userId))          // can't look at someone else's payment
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private static PaymentStatusDto toStatus(Payment p) {
        return new PaymentStatusDto(p.getRazorpayOrderId(), p.getExpenseId(),
                p.getAmount().setScale(2).toPlainString(), p.getStatus().name());
    }
}