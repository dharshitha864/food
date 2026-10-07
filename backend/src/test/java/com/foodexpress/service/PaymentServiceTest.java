package com.foodexpress.service;

import com.foodexpress.dto.PaymentDto;
import com.foodexpress.dto.PaymentRequest;
import com.foodexpress.entity.*;
import com.foodexpress.repository.OrderRepository;
import com.foodexpress.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Test User", "test@example.com", "pass", "9999999999", Role.ROLE_CUSTOMER, true);
        order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-1001");
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setFinalAmount(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Process Online Card payment successfully sets status to SUCCESS")
    void testProcessPayment_OnlineCard() {
        PaymentRequest req = new PaymentRequest(10L, PaymentMethod.CARD, "4111-XXXX-XXXX-1111", null);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentDto dto = paymentService.processPayment(req);

        assertNotNull(dto);
        assertEquals(PaymentStatus.SUCCESS, dto.getPaymentStatus());
        assertEquals(PaymentMethod.CARD, dto.getPaymentMethod());
        assertNotNull(dto.getTransactionId());
        verify(notificationService).createNotification(eq(1L), anyString(), anyString());
    }

    @Test
    @DisplayName("Process Cash on Delivery payment sets status to PENDING")
    void testProcessPayment_COD() {
        PaymentRequest req = new PaymentRequest(10L, PaymentMethod.COD, null, null);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentDto dto = paymentService.processPayment(req);

        assertEquals(PaymentStatus.PENDING, dto.getPaymentStatus());
        assertEquals(PaymentMethod.COD, dto.getPaymentMethod());
    }
}
