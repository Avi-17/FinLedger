package com.finledger.api.payment.api;

import com.finledger.api.payment.api.PaymentDtos.*;
import com.finledger.api.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/expenses/{expenseId}/pay")
    public PayResponse pay(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return paymentService.pay(userId, expenseId);
    }

    @PostMapping("/payments/verify")
    public PaymentStatusDto verify(@AuthenticationPrincipal Long userId, @Valid @RequestBody VerifyRequest req) {
        return paymentService.verify(userId, req);
    }

    @GetMapping("/payments/{orderId}")
    public PaymentStatusDto status(@AuthenticationPrincipal Long userId, @PathVariable String orderId) {
        return paymentService.status(userId, orderId);
    }
}