package com.finledger.api.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds app.razorpay.* from application.properties (values come from env vars). */
@ConfigurationProperties(prefix = "app.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret) {}