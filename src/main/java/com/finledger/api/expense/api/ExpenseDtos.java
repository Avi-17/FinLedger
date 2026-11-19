package com.finledger.api.expense.api;

import com.finledger.api.expense.domain.Expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ExpenseDtos {
    /** amount is required only for EXACT splits */
    public record Participant(@Schema(example = "2") @NotNull Long userId,
                              @Schema(description = "Only for EXACT splits", example = "1000.00") BigDecimal amount) {}

    public record CreateExpenseRequest(
            @Schema(example = "blinkit") @NotBlank @Size(max = 50) String description,
            @Schema(example = "3000.00") @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Expense.SplitType splitType,
            @NotEmpty List<@Valid Participant> participants) {}

    public record MarkPaidRequest(@Schema(example = "paid in cash") @Size(max = 100) String note) {}

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