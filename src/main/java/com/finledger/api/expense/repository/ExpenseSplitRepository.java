package com.finledger.api.expense.repository;

import com.finledger.api.expense.domain.ExpenseSplit;
import com.finledger.api.expense.domain.ExpenseSplitId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, ExpenseSplitId> {
    List<ExpenseSplit> findByExpenseId(Long expenseId);
    List<ExpenseSplit> findByExpenseIdIn(Collection<Long> expenseIds);
    long countByExpenseIdAndStatus(Long expenseId, ExpenseSplit.Status status);
}
