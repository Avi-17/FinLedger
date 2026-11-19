package com.finledger.api.common;

/** Thrown when the payment provider fails or is unreachable. */
public class PaymentGatewayException extends DomainException {
    public PaymentGatewayException(String message) { super(message); }
}