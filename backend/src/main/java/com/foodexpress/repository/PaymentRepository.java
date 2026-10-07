package com.foodexpress.repository;

import com.foodexpress.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByTransactionId(String transactionId);
}
