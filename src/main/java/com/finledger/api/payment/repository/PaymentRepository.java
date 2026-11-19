package com.finledger.api.payment.repository;

import com.finledger.api.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.razorpayOrderId = :orderId")
    Optional<Payment> lockByOrderId(String orderId);

    Optional<Payment> findFirstByExpenseIdAndUserIdAndStatus(Long expenseId, Long userId, Payment.Status status);
    Optional<Payment> findByRazorpayOrderId(String orderId);
}