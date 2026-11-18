package com.finledger.api.expense.domain;

import com.finledger.api.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** One person's share of an Expense. State machine: PENDING -> PAID (once). */
@Entity
@Table(name = "expense_splits")
@IdClass(ExpenseSplitId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseSplit {
    @Id @Column(name = "expense_id")
    private Long expenseId;

    @Id @Column(name = "user_id")
    private Long userId;

    @Column(name = "amount_owed", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountOwed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_via")
    private PaidVia paidVia;

    @Column(name = "paid_at")
    private Instant paidAt;

    private String note;

    public enum Status { PENDING, PAID }
    public enum PaidVia { SELF, RAZORPAY, CASH }

    public ExpenseSplit(Long expenseId, Long userId, BigDecimal amountOwed) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.amountOwed = amountOwed;
    }

    public boolean isPending() { return status == Status.PENDING; }
    public boolean isPaid() { return status == Status.PAID; }

    /** PENDING -> PAID. Throws if already paid, so a share can never be paid twice. */
    public void markPaid(PaidVia via, String note) {
        if (isPaid()) throw new BusinessRuleException("This share is already paid");
        this.status = Status.PAID;
        this.paidVia = via;
        this.paidAt = Instant.now();
        this.note = note;
    }
}
