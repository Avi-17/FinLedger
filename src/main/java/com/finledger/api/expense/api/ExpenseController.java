package com.finledger.api.expense.api;

import com.finledger.api.expense.service.ExpenseService;

import com.finledger.api.expense.api.ExpenseDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Expenses")
@RestController
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @Operation(summary = "Create a split request",
            description = "The caller is the requester (already paid the shop) and gets no Pay button. "
                    + "EQUAL: omit amounts, leftover paisa goes to the requester. EXACT: amounts must add up to the total.")
    @PostMapping("/groups/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDetailDto create(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                   @Valid @RequestBody CreateExpenseRequest req) {
        return expenseService.create(userId, groupId, req);
    }

    @Operation(summary = "Group feed", description = "Newest first, 20 per page. Each card shows x of y paid, paid/left, myShare and canPay.")
    @GetMapping("/groups/{groupId}/expenses")
    public List<ExpenseCardDto> feed(@AuthenticationPrincipal Long userId, @PathVariable Long groupId,
                                     @RequestParam(defaultValue = "0") int page) {
        return expenseService.feed(userId, groupId, page);
    }

    @Operation(summary = "Split request detail", description = "The card plus every share (pending first).")
    @GetMapping("/expenses/{expenseId}")
    public ExpenseDetailDto detail(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return expenseService.detail(userId, expenseId);
    }

    @Operation(summary = "Mark a share as paid (cash)", description = "Requester only. Settles the request when nobody is pending.")
    @PostMapping("/expenses/{expenseId}/splits/{debtorId}/mark-paid")
    public ExpenseDetailDto markPaid(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId,
                                     @PathVariable Long debtorId, @Valid @RequestBody(required = false) MarkPaidRequest req) {
        return expenseService.markPaid(userId, expenseId, debtorId, req == null ? null : req.note());
    }

    @Operation(summary = "Close the request", description = "Requester only. Not allowed once fully paid.")
    @PostMapping("/expenses/{expenseId}/close")
    public ExpenseDetailDto close(@AuthenticationPrincipal Long userId, @PathVariable Long expenseId) {
        return expenseService.close(userId, expenseId);
    }
}