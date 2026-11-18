package com.finledger.api.group.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "expense_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ExpenseGroup create(String name, Long createdBy) {
        ExpenseGroup g = new ExpenseGroup();
        g.name = name.trim();
        g.createdBy = createdBy;
        return g;
    }
}
