package com.foodexpress.service;

import com.foodexpress.dto.OrderDto;
import com.foodexpress.entity.Order;
import com.foodexpress.entity.OrderStatus;
import com.foodexpress.entity.Restaurant;
import com.foodexpress.entity.User;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.InvalidOrderStatusException;
import com.foodexpress.repository.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private FoodItemRepository foodItemRepository;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(10L);
        customer.setFullName("Ananya");
        customer.setEmail("ananya@example.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Spicy Bistro");

        sampleOrder = new Order();
        sampleOrder.setId(100L);
        sampleOrder.setOrderNumber("ORD-123456");
        sampleOrder.setUser(customer);
        sampleOrder.setRestaurant(restaurant);
        sampleOrder.setStatus(OrderStatus.PLACED);
        sampleOrder.setTotalAmount(new BigDecimal("300.00"));
        sampleOrder.setFinalAmount(new BigDecimal("355.00"));
    }

    @Test
    @DisplayName("Successfully update order status with valid transition")
    void testUpdateOrderStatus_ValidTransition() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderDto updated = orderService.updateOrderStatus(100L, OrderStatus.ACCEPTED);

        assertEquals(OrderStatus.ACCEPTED, sampleOrder.getStatus());
        verify(notificationService).createNotification(eq(10L), anyString(), anyString());
    }

    @Test
    @DisplayName("Throw InvalidOrderStatusException on illegal transition (PLACED to DELIVERED)")
    void testUpdateOrderStatus_IllegalTransition() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.updateOrderStatus(100L, OrderStatus.DELIVERED);
        });
    }

    @Test
    @DisplayName("Cancel order successfully when in PLACED state")
    void testCancelOrder_Success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderDto result = orderService.cancelOrder(100L, 10L, false);

        assertEquals(OrderStatus.CANCELLED, sampleOrder.getStatus());
        verify(paymentService).refundPayment(100L);
    }

    @Test
    @DisplayName("Disallow cancellation when order is already in OUT_FOR_DELIVERY state")
    void testCancelOrder_DisallowedState() {
        sampleOrder.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));

        assertThrows(BadRequestException.class, () -> {
            orderService.cancelOrder(100L, 10L, false);
        });
    }
}
