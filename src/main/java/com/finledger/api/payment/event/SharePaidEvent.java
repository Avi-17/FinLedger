package com.finledger.api.payment.event;

/** Published when Razorpay confirms that a user paid their share. Listeners react; the publisher doesn't know who they are. */
public record SharePaidEvent(Long expenseId, Long userId, String razorpayPaymentId) {}