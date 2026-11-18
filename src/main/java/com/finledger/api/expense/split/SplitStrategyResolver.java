package com.finledger.api.expense.split;

import com.finledger.api.common.BusinessRuleException;
import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.api.ExpenseDtos.Participant;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/**
 * Picks the right SplitStrategy for a SplitType. Spring injects every SplitStrategy bean,
 * so this class never changes when a new split type is added (Open/Closed).
 */
@Component
public class SplitStrategyResolver {
    private final Map<Expense.SplitType, SplitStrategy> strategies = new EnumMap<>(Expense.SplitType.class);

    public SplitStrategyResolver(List<SplitStrategy> all) {
        all.forEach(s -> strategies.put(s.type(), s));
    }

    public Map<Long, BigDecimal> split(Expense.SplitType type, BigDecimal total,
                                       List<Participant> participants, Long requesterId) {
        SplitStrategy strategy = strategies.get(type);
        if (strategy == null) throw new BusinessRuleException("Unsupported split type: " + type);

        Map<Long, BigDecimal> shares = strategy.split(total, participants, requesterId);
        if (shares.values().stream().anyMatch(v -> v.signum() <= 0))
            throw new BusinessRuleException("Amount too small to split between " + participants.size() + " people");
        return shares;
    }
}
