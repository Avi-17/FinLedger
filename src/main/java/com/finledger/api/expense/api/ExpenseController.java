package com.finledger.api.expense.api;

import com.finledger.api.expense.service.ExpenseService;

import com.finledger.api.expense.api.ExpenseDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping("/groups/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDetailDto create(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                   @Valid @RequestBody CreateExpenseRequest req) {
        return expenseService.create(userId, groupId, req);
    }

    @GetMapping("/groups/{groupId}/expenses")
    public List<ExpenseCardDto> feed(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                     @RequestParam(defaultValue = "0") int page) {
        return expenseService.feed(userId, groupId, page);
    }

    @GetMapping("/expenses/{expenseId}")
    public ExpenseDetailDto detail(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return expenseService.detail(userId, expenseId);
    }

    @PostMapping("/expenses/{expenseId}/splits/{debtorId}/mark-paid")
    public ExpenseDetailDto markPaid(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId,
                                     @PathVariable Long debtorId, @Valid @RequestBody(required = false) MarkPaidRequest req) {
        return expenseService.markPaid(userId, expenseId, debtorId, req == null ? null : req.note());
    }

    @PostMapping("/expenses/{expenseId}/close")
    public ExpenseDetailDto close(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return expenseService.close(userId, expenseId);
    }
}