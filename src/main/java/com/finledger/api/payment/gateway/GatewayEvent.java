package com.finledger.api.payment.gateway;

public record GatewayEvent(Type type, String orderId, String paymentId, long amountPaise) {
    public enum Type { CAPTURED, FAILED, IGNORED }

    public static GatewayEvent ignored() {
        return new GatewayEvent(Type.IGNORED, null, null, 0);
    }
}