package com.finledger.api.expense.event;

import com.finledger.api.expense.service.ExpenseService;
import com.finledger.api.payment.event.SharePaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Marks the share PAID when a payment is captured. Plain @EventListener runs synchronously inside the
 * webhook's transaction, so "payment CAPTURED" and "share PAID" commit together or not at all.
 */
@Component
@RequiredArgsConstructor
public class SharePaidListener {
    private final ExpenseService expenseService;

    @EventListener
    public void on(SharePaidEvent e) {
        expenseService.recordOnlinePayment(e.expenseId(), e.userId());
    }
}