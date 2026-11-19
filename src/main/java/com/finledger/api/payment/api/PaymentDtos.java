package com.finledger.api.payment.api;

import jakarta.validation.constraints.NotBlank;

public class PaymentDtos {
    /** Everything the app needs to open Razorpay Checkout. */
    public record PayResponse(
            String orderId,
            long amountPaise,      // Checkout expects paise
            String amount,         // "107.50", for display
            String currency,
            String keyId,          // public key, safe for the client
            String description) {}

    public record VerifyRequest(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) {}

    public record PaymentStatusDto(String orderId, Long expenseId, String amount, String status) {}
}