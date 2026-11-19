package com.finledger.api.payment.api;

import com.finledger.api.payment.api.PaymentDtos.*;
import com.finledger.api.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments")
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Pay my share: create Razorpay order",
            description = "Returns orderId, amount and the public keyId needed to open Razorpay Checkout. A repeat call returns the same order.")
    @PostMapping("/expenses/{expenseId}/pay")
    public PayResponse pay(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return paymentService.pay(userId, expenseId);
    }

    @Operation(summary = "Verify Checkout result",
            description = "Checks the HMAC signature. If the webhook hasn't arrived yet, asks Razorpay directly and records the payment (reconciliation).")
    @PostMapping("/payments/verify")
    public PaymentStatusDto verify(@AuthenticationPrincipal Long userId, @Valid @RequestBody VerifyRequest req) {
        return paymentService.verify(userId, req);
    }

    @Operation(summary = "Payment status (poll)", description = "CREATED until Razorpay confirms, then CAPTURED.")
    @GetMapping("/payments/{orderId}")
    public PaymentStatusDto status(@AuthenticationPrincipal Long userId, @PathVariable String orderId) {
        return paymentService.status(userId, orderId);
    }
}