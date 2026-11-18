package com.finledger.api.expense.split;

import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.api.ExpenseDtos.Participant;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public Expense.SplitType type() { return Expense.SplitType.EQUAL; }

    @Override
    public Map<Long, BigDecimal> split(BigDecimal total, List<Participant> participants, Long requesterId) {
        int n = participants.size();
        BigDecimal each = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        participants.forEach(p -> out.put(p.userId(), each));

        BigDecimal remainder = total.subtract(each.multiply(BigDecimal.valueOf(n)));
        if (remainder.signum() > 0) {
            Long who = out.containsKey(requesterId) ? requesterId : participants.get(0).userId();
            out.merge(who, remainder, BigDecimal::add);
        }
        return out;
    }
}
