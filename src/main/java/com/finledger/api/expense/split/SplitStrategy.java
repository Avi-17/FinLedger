package com.finledger.api.expense.split;

import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.api.ExpenseDtos.Participant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Strategy pattern: one implementation per split type. Add a new type = add a new @Component. */
public interface SplitStrategy {
    Expense.SplitType type();

    /** Returns userId -> share, in participant order. Shares must add up to total. */
    Map<Long, BigDecimal> split(BigDecimal total, List<Participant> participants, Long requesterId);
}
