package com.finledger.api.expense.service;

import com.finledger.api.expense.domain.Expense;
import com.finledger.api.expense.domain.ExpenseSplit;

import com.finledger.api.expense.api.ExpenseDtos.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Entity -> DTO conversion for split requests */
@Component
public class ExpenseMapper {

    /** Card shown in the feed, from the point of view of user `me`. */
    public ExpenseCardDto toCard(Expense e, List<ExpenseSplit> splits, Long me, Map<Long, String> names) {
        int paidCount = 0;
        BigDecimal paid = BigDecimal.ZERO;
        ExpenseSplit mine = null;
        for (ExpenseSplit s : splits) {
            if (s.isPaid()) { paidCount++; paid = paid.add(s.getAmountOwed()); }
            if (s.getUserId().equals(me)) mine = s;
        }
        boolean isRequester = e.isRequester(me);
        boolean canPay = !isRequester && e.isOpen() && mine != null && mine.isPending();

        return new ExpenseCardDto(e.getId(), e.getDescription(), money(e.getAmount()),
                e.getPaidBy(), names.get(e.getPaidBy()), e.getStatus().name(),
                paidCount, splits.size(), money(paid), money(e.getAmount().subtract(paid)),
                mine == null ? null : money(mine.getAmountOwed()),
                mine == null ? null : mine.getStatus().name(),
                isRequester, canPay, e.getCreatedAt());
    }

    /** Detail screen: the card plus every share, pending first. */
    public ExpenseDetailDto toDetail(Expense e, List<ExpenseSplit> splits, Long me, Map<Long, String> names) {
        List<SplitDto> rows = splits.stream()
                .sorted(Comparator.comparing(ExpenseSplit::getStatus).thenComparing(ExpenseSplit::getUserId))
                .map(s -> new SplitDto(s.getUserId(), names.get(s.getUserId()), money(s.getAmountOwed()),
                        s.getStatus().name(), s.getPaidVia() == null ? null : s.getPaidVia().name(),
                        s.getPaidAt(), s.getNote()))
                .toList();
        return new ExpenseDetailDto(toCard(e, splits, me, names), rows);
    }

    /** Money goes to the client as a string with exactly 2 decimals, e.g. "107.50". */
    public static String money(BigDecimal b) { return b.setScale(2, RoundingMode.UNNECESSARY).toPlainString(); }
}
