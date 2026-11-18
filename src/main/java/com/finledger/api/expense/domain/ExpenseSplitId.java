package com.finledger.api.expense.domain;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplitId implements Serializable {
    private Long expenseId;
    private Long userId;
}