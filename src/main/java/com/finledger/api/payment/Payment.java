package com.finledger.api.payment;

import com.finledger.api.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

/** One Razorpay order for one share. State machine: CREATED -> CAPTURED or CREATED -> FAILED. */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status { CREATED, CAPTURED, FAILED }

    public static Payment create(Long expenseId, Long userId, String razorpayOrderId, BigDecimal amount) {
        Payment p = new Payment();
        p.expenseId = expenseId;
        p.userId = userId;
        p.razorpayOrderId = razorpayOrderId;
        p.amount = amount;
        return p;
    }

    public boolean isCreated() { return status == Status.CREATED; }

    /** CREATED -> CAPTURED. */
    public void capture(String razorpayPaymentId) {
        if (!isCreated()) throw new BusinessRuleException("Payment is already " + status);
        this.razorpayPaymentId = razorpayPaymentId;
        this.status = Status.CAPTURED;
    }

    /** CREATED -> FAILED. */
    public void fail() {
        if (!isCreated()) throw new BusinessRuleException("Payment is already " + status);
        this.status = Status.FAILED;
    }
}
