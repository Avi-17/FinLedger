package com.finledger.api.expense.service;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.domain.ExpenseSplit;
import com.finledger.api.expense.domain.ExpenseSplitId;
import com.finledger.api.expense.repository.ExpenseRepository;
import com.finledger.api.expense.repository.ExpenseSplitRepository;
import com.finledger.api.group.access.GroupAccessPolicy;

import com.finledger.api.common.ForbiddenException;
import com.finledger.api.common.NotFoundException;
import com.finledger.api.expense.api.ExpenseDtos.*;
import com.finledger.api.expense.split.SplitStrategyResolver;
import com.finledger.api.user.repository.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Split-request use cases. Orchestrates only: validation -> ExpenseValidator, shares -> SplitStrategy,
 * status rules -> entity methods, DTOs -> ExpenseMapper, access -> GroupAccessPolicy.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {
    private static final int PAGE_SIZE = 20;

    private final ExpenseRepository expenses;
    private final ExpenseSplitRepository splits;
    private final ExpenseValidator validator;
    private final SplitStrategyResolver splitResolver;
    private final GroupAccessPolicy access;
    private final ExpenseMapper mapper;
    private final UserDirectory userDirectory;

    @Transactional
    public ExpenseDetailDto create(Long userId, Long groupId, CreateExpenseRequest req) {
        validator.validateNew(groupId, userId, req);
        Map<Long, BigDecimal> shares = splitResolver.split(req.splitType(), req.amount(), req.participants(), userId);

        Expense e = expenses.save(Expense.create(groupId, userId, req.description(), req.amount(), req.splitType()));
        shares.forEach((uid, amt) -> {
            ExpenseSplit s = new ExpenseSplit(e.getId(), uid, amt);
            if (e.isRequester(uid)) s.markPaid(ExpenseSplit.PaidVia.SELF, null);   // requester already paid the shop
            splits.save(s);
        });
        return detail(userId, e.getId());
    }

    @Transactional(readOnly = true)
    public List<ExpenseCardDto> feed(Long userId, Long groupId, int page) {
        access.requireMember(groupId, userId);
        List<Expense> list = expenses.findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(page, PAGE_SIZE)).getContent();
        if (list.isEmpty()) return List.of();

        Map<Long, List<ExpenseSplit>> byExpense = splits.findByExpenseIdIn(list.stream().map(Expense::getId).toList())
                .stream().collect(Collectors.groupingBy(ExpenseSplit::getExpenseId));
        Map<Long, String> names = userDirectory.namesByIds(list.stream().map(Expense::getPaidBy).toList());

        return list.stream()
                .map(e -> mapper.toCard(e, byExpense.getOrDefault(e.getId(), List.of()), userId, names))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDetailDto detail(Long userId, Long expenseId) {
        Expense e = expenses.findById(expenseId).orElseThrow(() -> new NotFoundException("Expense not found"));
        access.requireMember(e.getGroupId(), userId);

        List<ExpenseSplit> ss = splits.findByExpenseId(expenseId);
        Set<Long> ids = ss.stream().map(ExpenseSplit::getUserId).collect(Collectors.toSet());
        ids.add(e.getPaidBy());
        return mapper.toDetail(e, ss, userId, userDirectory.namesByIds(ids));
    }

    /** Requester records a share as paid outside the app (cash/UPI elsewhere). */
    @Transactional
    public ExpenseDetailDto markPaid(Long userId, Long expenseId, Long debtorId, String note) {
        Expense e = lockAsRequester(userId, expenseId);
        e.requireOpen();
        ExpenseSplit s = splits.findById(new ExpenseSplitId(expenseId, debtorId))
                .orElseThrow(() -> new NotFoundException("That person is not part of this split"));
        s.markPaid(ExpenseSplit.PaidVia.CASH, note);
        settleIfComplete(e);
        return detail(userId, expenseId);
    }

    @Transactional
    public ExpenseDetailDto close(Long userId, Long expenseId) {
        lockAsRequester(userId, expenseId).close();
        return detail(userId, expenseId);
    }

    /** Call after any share becomes PAID, with the expense row locked. Reused by the payment webhook. */
    private void settleIfComplete(Expense e) {
        splits.flush();   // make sure the share just marked PAID is counted
        if (e.isOpen() && splits.countByExpenseIdAndStatus(e.getId(), ExpenseSplit.Status.PENDING) == 0)
            e.settle();
    }

    private Expense lockAsRequester(Long userId, Long expenseId) {
        Expense e = expenses.lockById(expenseId).orElseThrow(() -> new NotFoundException("Expense not found"));
        if (!e.isRequester(userId)) throw new ForbiddenException("Only the requester can do this");
        return e;
    }

    /**
     * Used by the payment module before creating an order. Locks the expense row so two taps
     * can't create two orders at once. Returns the caller's unpaid share.
     */
    @Transactional
    public ExpenseSplit lockPayableShare(Long userId, Long expenseId) {
        Expense e = expenses.lockById(expenseId).orElseThrow(() -> new NotFoundException("Expense not found"));
        access.requireMember(e.getGroupId(), userId);
        if (e.isRequester(userId)) throw new BusinessRuleException("You created this request, there is nothing to pay");
        e.requireOpen();

        ExpenseSplit s = splits.findById(new ExpenseSplitId(expenseId, userId))
                .orElseThrow(() -> new BusinessRuleException("You are not part of this split"));
        if (!s.isPending()) throw new BusinessRuleException("Your share is already paid");
        return s;
    }

    /** Used by the payment module once Razorpay confirms a capture. Safe to call twice. */
    @Transactional
    public void recordOnlinePayment(Long expenseId, Long userId) {
        Expense e = expenses.lockById(expenseId).orElseThrow(() -> new NotFoundException("Expense not found"));
        ExpenseSplit s = splits.findById(new ExpenseSplitId(expenseId, userId))
                .orElseThrow(() -> new NotFoundException("Share not found"));
        if (!s.isPending()) return;                       // already PAID (e.g. marked as cash meanwhile)
        s.markPaid(ExpenseSplit.PaidVia.RAZORPAY, null);
        settleIfComplete(e);
    }
}
