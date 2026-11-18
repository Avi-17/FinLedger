package com.finledger.api.expense.api;

import com.finledger.api.expense.domain.Expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ExpenseDtos {
    /** amount is required only for EXACT splits */
    public record Participant(@NotNull Long userId, BigDecimal amount) {}

    public record CreateExpenseRequest(
            @NotBlank @Size(max = 50) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Expense.SplitType splitType,
            @NotEmpty List<@Valid Participant> participants) {}

    public record MarkPaidRequest(@Size(max = 100) String note) {}

    public record ExpenseCardDto(
            Long id, String description, String amount,
            Long requesterId, String requesterName, String status,
            int paidCount, int totalCount, String paidAmount, String leftAmount,
            String myShare, String myStatus, boolean isRequester, boolean canPay,
            Instant createdAt) {}

    public record SplitDto(Long userId, String name, String amount, String status,
                           String paidVia, Instant paidAt, String note) {}

    public record ExpenseDetailDto(ExpenseCardDto summary, List<SplitDto> splits) {}
}