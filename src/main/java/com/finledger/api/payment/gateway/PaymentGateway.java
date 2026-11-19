package com.finledger.api.payment.gateway;

import java.math.BigDecimal;

/**
 * Adapter pattern: the rest of the app talks to this interface, never to the Razorpay SDK.
 */
public interface PaymentGateway {
    /** Creates an order for `amount` rupees. Returns the provider's order id. */
    String createOrder(BigDecimal amount, String receipt);

    /** Checks the signature Checkout returns to the app after a payment. */
    boolean isPaymentSignatureValid(String orderId, String paymentId, String signature);

    /** Checks the X-Razorpay-Signature header of a webhook against its raw body. */
    boolean isWebhookSignatureValid(String rawBody, String signature);

    /** Public key the app needs to open Checkout. Safe to send to the client. */
    String publicKey();

    /** Turns a (signature-checked) webhook body into a GatewayEvent. */
    GatewayEvent parseWebhook(String rawBody);

    /**
     * Asks the provider for the current state of a payment (used for reconciliation when a webhook is late).
     * Returns CAPTURED / FAILED, or IGNORED if it is still in progress or the provider can't be reached.
     */
    GatewayEvent fetchPayment(String paymentId);
}