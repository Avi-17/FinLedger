package com.finledger.api.payment.api;

import com.finledger.api.payment.gateway.PaymentGateway;
import com.finledger.api.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoint (permitted in SecurityConfig) — trust comes from the HMAC signature, not a JWT.
 * Body is taken as a raw String: the signature is computed over the exact bytes, so it must not be re-serialized.
 */
@Tag(name = "Webhooks")
@Slf4j
@RestController
@RequiredArgsConstructor
public class RazorpayWebhookController {
    private final PaymentGateway gateway;
    private final PaymentService paymentService;

    @Operation(summary = "Razorpay webhook",
            description = "Trusted via the X-Razorpay-Signature HMAC header, not a JWT. Idempotent: duplicate deliveries are ignored.")
    @SecurityRequirements
    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> handle(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (!gateway.isWebhookSignatureValid(rawBody, signature)) {
            log.warn("Rejected webhook with bad signature");
            return ResponseEntity.badRequest().build();
        }
        paymentService.handleWebhook(gateway.parseWebhook(rawBody));
        return ResponseEntity.ok().build();   // 200 = "received", or Razorpay keeps retrying
    }
}