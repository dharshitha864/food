package com.foodexpress.service;

import com.foodexpress.dto.PaymentDto;
import com.foodexpress.dto.PaymentRequest;
import com.foodexpress.entity.*;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.OrderRepository;
import com.foodexpress.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    public PaymentDto processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw new BadRequestException("Cannot process payment for an inactive order");
        }

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Order has already been paid successfully");
        }

        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        payment.setPaymentId(paymentId);
        payment.setTransactionId(txnId);
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(request.getPaymentMethod());

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            payment.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            notificationService.createNotification(
                    order.getUser().getId(),
                    "Payment Successful",
                    "Payment of ₹" + order.getFinalAmount() + " for Order #" + order.getOrderNumber() + " was processed successfully."
            );
        }

        payment.setPaymentDate(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order ID: " + orderId));
        return mapToDto(payment);
    }

    public PaymentDto refundPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            notificationService.createNotification(
                    payment.getOrder().getUser().getId(),
                    "Refund Processed",
                    "Refund of ₹" + payment.getAmount() + " for Order #" + payment.getOrder().getOrderNumber() + " has been initiated."
            );
        }

        return mapToDto(payment);
    }

    public PaymentDto mapToDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getPaymentId(),
                payment.getTransactionId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaymentDate()
        );
    }
}
