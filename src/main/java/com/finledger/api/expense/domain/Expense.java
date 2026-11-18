package com.finledger.api.expense.domain;

import com.finledger.api.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One split request ("blinkit, ₹3000"). Status changes only through close()/settle(),
 * which enforce the state machine: OPEN -> SETTLED, OPEN -> CLOSED. No other moves.
 */
@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // for JPA only
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "paid_by", nullable = false)
    private Long paidBy;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum Status { OPEN, SETTLED, CLOSED }
    public enum SplitType { EQUAL, EXACT }

    /** Factory method: the requester is both the payer and the creator. */
    public static Expense create(Long groupId, Long requesterId, String description,
                                 BigDecimal amount, SplitType splitType) {
        Expense e = new Expense();
        e.groupId = groupId;
        e.paidBy = requesterId;
        e.createdBy = requesterId;
        e.description = description.trim();
        e.amount = amount.setScale(2);
        e.splitType = splitType;
        return e;
    }

    public boolean isOpen() { return status == Status.OPEN; }
    public boolean isRequester(Long userId) { return paidBy.equals(userId); }

    /** OPEN -> CLOSED. Not allowed once everyone has paid. Closing twice is a no-op. */
    public void close() {
        if (status == Status.SETTLED) throw new BusinessRuleException("Already fully paid");
        status = Status.CLOSED;
    }

    /** OPEN -> SETTLED, called when no share is PENDING any more. */
    public void settle() {
        if (status != Status.OPEN) throw new BusinessRuleException("Only an open request can be settled");
        status = Status.SETTLED;
    }

    /** Guard used before recording any payment against this request. */
    public void requireOpen() {
        if (!isOpen()) throw new BusinessRuleException("Request is " + status.name().toLowerCase());
    }
}
