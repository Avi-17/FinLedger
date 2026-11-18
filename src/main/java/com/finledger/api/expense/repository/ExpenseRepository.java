package com.finledger.api.expense.repository;

import com.finledger.api.expense.domain.Expense;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Expense e where e.id = :id")
    Optional<Expense> lockById(Long id);
}