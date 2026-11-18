package com.finledger.api.expense.split;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.api.ExpenseDtos.Participant;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public Expense.SplitType type() { return Expense.SplitType.EXACT; }

    @Override
    public Map<Long, BigDecimal> split(BigDecimal total, List<Participant> participants, Long requesterId) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        for (Participant p : participants) {
            if (p.amount() == null || p.amount().signum() <= 0)
                throw new BusinessRuleException("Every participant needs an amount greater than 0");
            if (p.amount().scale() > 2)
                throw new BusinessRuleException("Amounts can have at most 2 decimal places");
            out.put(p.userId(), p.amount().setScale(2));
        }
        BigDecimal sum = out.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total) != 0)
            throw new BusinessRuleException("Split amounts add up to " + sum + ", expected " + total);
        return out;
    }
}
