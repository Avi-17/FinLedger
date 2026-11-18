package com.finledger.api.expense.service;

import com.finledger.api.group.access.GroupAccessPolicy;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.expense.api.ExpenseDtos.CreateExpenseRequest;
import com.finledger.api.expense.api.ExpenseDtos.Participant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;

/** Rules a new split request must satisfy before any shares are calculated. */
@Component
@RequiredArgsConstructor
public class ExpenseValidator {
    private final GroupAccessPolicy access;

    public void validateNew(Long groupId, Long requesterId, CreateExpenseRequest req) {
        access.requireMember(groupId, requesterId);

        List<Long> ids = req.participants().stream().map(Participant::userId).toList();
        if (new HashSet<>(ids).size() != ids.size())
            throw new BusinessRuleException("A person can appear only once in a split");
        if (ids.stream().allMatch(requesterId::equals))
            throw new BusinessRuleException("Add at least one other person to the split");
        access.requireAllMembers(groupId, ids);
    }
}
