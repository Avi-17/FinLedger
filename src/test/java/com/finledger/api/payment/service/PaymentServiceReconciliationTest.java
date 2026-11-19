package com.finledger.api.payment.service;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.common.NotFoundException;
import com.finledger.api.expense.service.ExpenseService;
import com.finledger.api.payment.api.PaymentDtos.VerifyRequest;
import com.finledger.api.payment.domain.Payment;
import com.finledger.api.payment.event.SharePaidEvent;
import com.finledger.api.payment.gateway.GatewayEvent;
import com.finledger.api.payment.gateway.PaymentGateway;
import com.finledger.api.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PaymentService with a fake PaymentGateway: no Razorpay, no database.
 * Possible only because the service depends on the PaymentGateway interface (Dependency Inversion).
 */
class PaymentServiceReconciliationTest {
    static final String ORDER = "order_1", PAY = "pay_1", SIG = "sig";
    static final long PRIYA = 2L, EXPENSE = 5L, PAISE = 180000;

    PaymentRepository payments = mock(PaymentRepository.class);
    PaymentGateway gateway = mock(PaymentGateway.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    PaymentService service = new PaymentService(mock(ExpenseService.class), payments, gateway, events);

    Payment payment;
    VerifyRequest req = new VerifyRequest(ORDER, PAY, SIG);

    @BeforeEach
    void setUp() {
        payment = Payment.create(EXPENSE, PRIYA, ORDER, new BigDecimal("1800.00"));
        when(payments.lockByOrderId(ORDER)).thenReturn(Optional.of(payment));
        when(gateway.isPaymentSignatureValid(ORDER, PAY, SIG)).thenReturn(true);
    }

    GatewayEvent captured(String orderId, long paise) {
        return new GatewayEvent(GatewayEvent.Type.CAPTURED, orderId, PAY, paise);
    }

    @Test
    void verify_capturesThroughReconciliation_whenWebhookIsLate() {
        when(gateway.fetchPayment(PAY)).thenReturn(captured(ORDER, PAISE));

        var status = service.verify(PRIYA, req);

        assertThat(status.status()).isEqualTo("CAPTURED");
        assertThat(payment.getRazorpayPaymentId()).isEqualTo(PAY);
        verify(events).publishEvent(new SharePaidEvent(EXPENSE, PRIYA, PAY));
    }

    @Test
    void webhookArrivingAfterReconciliation_isIgnored() {
        when(gateway.fetchPayment(PAY)).thenReturn(captured(ORDER, PAISE));
        service.verify(PRIYA, req);

        service.handleWebhook(captured(ORDER, PAISE));      // the late webhook finally shows up

        verify(events, times(1)).publishEvent(any(Object.class));        // share is paid exactly once
    }

    @Test
    void verify_afterWebhookAlreadyCaptured_doesNotAskRazorpay() {
        service.handleWebhook(captured(ORDER, PAISE));

        var status = service.verify(PRIYA, req);

        assertThat(status.status()).isEqualTo("CAPTURED");
        verify(gateway, never()).fetchPayment(anyString());
        verify(events, times(1)).publishEvent(any(Object.class));
    }

    @Test
    void verify_whileRazorpayStillProcessing_leavesPaymentCreated() {
        when(gateway.fetchPayment(PAY)).thenReturn(GatewayEvent.ignored());

        assertThat(service.verify(PRIYA, req).status()).isEqualTo("CREATED");
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void verify_rejectsInvalidSignature_withoutCallingRazorpay() {
        when(gateway.isPaymentSignatureValid(ORDER, PAY, SIG)).thenReturn(false);

        assertThatThrownBy(() -> service.verify(PRIYA, req)).isInstanceOf(BusinessRuleException.class);
        verify(gateway, never()).fetchPayment(anyString());
    }

    @Test
    void verify_rejectsPaymentThatBelongsToAnotherOrder() {
        when(gateway.fetchPayment(PAY)).thenReturn(captured("order_other", PAISE));

        assertThatThrownBy(() -> service.verify(PRIYA, req)).isInstanceOf(BusinessRuleException.class);
        assertThat(payment.isCaptured()).isFalse();
    }

    @Test
    void verify_doesNotCapture_whenAmountDiffers() {
        when(gateway.fetchPayment(PAY)).thenReturn(captured(ORDER, 100));   // ₹1, not ₹1800

        assertThat(service.verify(PRIYA, req).status()).isEqualTo("CREATED");
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void verify_onSomeoneElsesOrder_isNotFound() {
        assertThatThrownBy(() -> service.verify(99L, req)).isInstanceOf(NotFoundException.class);
        verify(gateway, never()).isPaymentSignatureValid(anyString(), anyString(), anyString());
    }

    @Test
    void verify_recordsFailedPayment() {
        when(gateway.fetchPayment(PAY)).thenReturn(new GatewayEvent(GatewayEvent.Type.FAILED, ORDER, PAY, PAISE));

        assertThat(service.verify(PRIYA, req).status()).isEqualTo("FAILED");
        verify(events, never()).publishEvent(any(Object.class));
    }
}
