package com.foodexpress.service;

import com.foodexpress.dto.*;
import com.foodexpress.entity.*;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.InvalidOrderStatusException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final DeliveryRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        AddressRepository addressRepository,
                        CouponService couponService,
                        NotificationService notificationService,
                        PaymentService paymentService,
                        DeliveryRepository deliveryRepository,
                        RestaurantRepository restaurantRepository,
                        FoodItemRepository foodItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.deliveryRepository = deliveryRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
    }

    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No active cart found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty. Add items before placing an order.");
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid delivery address selected"));

        Restaurant restaurant = cartItems.get(0).getFoodItem().getRestaurant();

        // Calculate Subtotal
        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Delivery & Tax
        BigDecimal deliveryFee = new BigDecimal("40.00");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

        // Calculate Coupon Discount
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                discount = couponService.calculateDiscount(request.getCouponCode(), subtotal);
            } catch (Exception e) {
                // Ignore invalid coupon on place order or throw
                discount = BigDecimal.ZERO;
            }
        }

        BigDecimal finalAmount = subtotal.add(deliveryFee).add(tax).subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // Generate human-friendly order number: e.g. ORD-2026-89431
        int randomSuffix = 10000 + new Random().nextInt(90000);
        String orderNumber = "ORD-" + System.currentTimeMillis() % 1000000 + "-" + randomSuffix;

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setDiscountAmount(discount);
        order.setFinalAmount(finalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setSpecialInstructions(request.getSpecialInstructions());

        Order savedOrder = orderRepository.save(order);

        // Convert cart items to order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem(
                    null,
                    savedOrder,
                    ci.getFoodItem(),
                    ci.getQuantity(),
                    ci.getUnitPrice(),
                    ci.getSubtotal()
            );
            orderItems.add(orderItemRepository.save(oi));
        }
        savedOrder.setOrderItems(orderItems);

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setRestaurant(null);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        // Create mock payment record
        PaymentRequest payReq = new PaymentRequest(
                savedOrder.getId(),
                request.getPaymentMethod(),
                "4111-XXXX-XXXX-1111",
                "customer@okaxis"
        );
        paymentService.processPayment(payReq);

        // Create delivery entry
        Delivery delivery = new Delivery(null, savedOrder, null, DeliveryStatus.PENDING);
        deliveryRepository.save(delivery);

        // Notify customer
        notificationService.createNotification(
                user.getId(),
                "Order Placed Successfully",
                "Your Order #" + orderNumber + " for ₹" + savedOrder.getFinalAmount() + " has been placed with " + restaurant.getName()
        );

        // Notify restaurant owner
        if (restaurant.getOwner() != null) {
            notificationService.createNotification(
                    restaurant.getOwner().getId(),
                    "New Order Received!",
                    "You received a new Order #" + orderNumber + " with " + orderItems.size() + " items."
            );
        }

        return mapToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getCustomerOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getRestaurantOrders(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        OrderStatus current = order.getStatus();

        // Terminal status check
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED || current == OrderStatus.REJECTED) {
            throw new InvalidOrderStatusException("Cannot change status of a completed/terminal order (" + current + ")");
        }

        // Validate state transitions
        validateStatusTransition(current, newStatus);

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        // Emit notifications on status changes
        String message = getStatusChangeMessage(order.getOrderNumber(), newStatus);
        notificationService.createNotification(order.getUser().getId(), "Order Status Update", message);

        return mapToDto(updated);
    }

    public OrderDto cancelOrder(Long orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException("Order cannot be cancelled in state: " + order.getStatus() + ". Preparation has begun.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        // Process mock refund if paid online
        paymentService.refundPayment(order.getId());

        notificationService.createNotification(
                order.getUser().getId(),
                "Order Cancelled",
                "Your Order #" + order.getOrderNumber() + " has been cancelled. Any amount paid will be refunded."
        );

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getAdminDashboardStats() {
        long customers = userRepository.findByRole(Role.ROLE_CUSTOMER).size();
        long restaurants = restaurantRepository.count();
        long couriers = userRepository.findByRole(Role.ROLE_DELIVERY_PARTNER).size();
        List<Order> allOrders = orderRepository.findAll();

        long totalOrders = allOrders.size();
        long activeOrders = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED &&
                             o.getStatus() != OrderStatus.CANCELLED &&
                             o.getStatus() != OrderStatus.REJECTED)
                .count();

        BigDecimal revenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long foodItems = foodItemRepository.count();

        return new AdminDashboardStatsDto(customers, restaurants, couriers, totalOrders, activeOrders, revenue, foodItems);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = false;

        switch (current) {
            case PLACED:
                valid = (next == OrderStatus.ACCEPTED || next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED);
                break;
            case ACCEPTED:
                valid = (next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED);
                break;
            case PREPARING:
                valid = (next == OrderStatus.READY_FOR_PICKUP);
                break;
            case READY_FOR_PICKUP:
                valid = (next == OrderStatus.DELIVERY_PARTNER_ASSIGNED || next == OrderStatus.PICKED_UP);
                break;
            case DELIVERY_PARTNER_ASSIGNED:
                valid = (next == OrderStatus.PICKED_UP);
                break;
            case PICKED_UP:
                valid = (next == OrderStatus.OUT_FOR_DELIVERY);
                break;
            case OUT_FOR_DELIVERY:
                valid = (next == OrderStatus.DELIVERED);
                break;
            default:
                valid = false;
        }

        if (!valid) {
            throw new InvalidOrderStatusException("Transition from " + current + " to " + next + " is not permitted.");
        }
    }

    private String getStatusChangeMessage(String orderNumber, OrderStatus status) {
        return switch (status) {
            case ACCEPTED -> "Your Order #" + orderNumber + " has been accepted by the restaurant.";
            case PREPARING -> "The kitchen has started preparing your Order #" + orderNumber + ".";
            case READY_FOR_PICKUP -> "Your Order #" + orderNumber + " is ready for pickup!";
            case PICKED_UP -> "Your Order #" + orderNumber + " was picked up by the delivery partner.";
            case OUT_FOR_DELIVERY -> "Your Order #" + orderNumber + " is out for delivery!";
            case DELIVERED -> "Your Order #" + orderNumber + " has been delivered! Enjoy your meal!";
            case REJECTED -> "Your Order #" + orderNumber + " was rejected by the restaurant.";
            case CANCELLED -> "Your Order #" + orderNumber + " was cancelled.";
            default -> "Status updated to " + status + " for Order #" + orderNumber;
        };
    }

    public OrderDto mapToDto(Order o) {
        OrderDto dto = new OrderDto();
        dto.setId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());
        dto.setUserId(o.getUser() != null ? o.getUser().getId() : null);
        dto.setCustomerName(o.getUser() != null ? o.getUser().getFullName() : null);
        dto.setCustomerEmail(o.getUser() != null ? o.getUser().getEmail() : null);
        dto.setCustomerPhone(o.getUser() != null ? o.getUser().getPhone() : null);
        dto.setRestaurantId(o.getRestaurant() != null ? o.getRestaurant().getId() : null);
        dto.setRestaurantName(o.getRestaurant() != null ? o.getRestaurant().getName() : null);

        if (o.getAddress() != null) {
            dto.setAddress(new AddressDto(
                    o.getAddress().getId(),
                    o.getAddress().getStreet(),
                    o.getAddress().getCity(),
                    o.getAddress().getState(),
                    o.getAddress().getPostalCode(),
                    o.getAddress().isDefault()
            ));
        }

        dto.setStatus(o.getStatus());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setDeliveryFee(o.getDeliveryFee());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setSpecialInstructions(o.getSpecialInstructions());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());

        List<OrderItemDto> itemDtos = new ArrayList<>();
        if (o.getOrderItems() != null) {
            for (OrderItem oi : o.getOrderItems()) {
                itemDtos.add(new OrderItemDto(
                        oi.getId(),
                        oi.getFoodItem().getId(),
                        oi.getFoodItem().getName(),
                        oi.getFoodItem().getImageUrl(),
                        oi.getFoodItem().isVeg(),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getSubtotal()
                ));
            }
        }
        dto.setItems(itemDtos);

        if (o.getPayment() != null) {
            dto.setPayment(paymentService.mapToDto(o.getPayment()));
        }

        if (o.getDelivery() != null) {
            Delivery d = o.getDelivery();
            dto.setDelivery(new DeliveryDto(
                    d.getId(),
                    o.getId(),
                    o.getOrderNumber(),
                    d.getDeliveryPartner() != null ? d.getDeliveryPartner().getId() : null,
                    d.getDeliveryPartner() != null ? d.getDeliveryPartner().getFullName() : null,
                    d.getDeliveryPartner() != null ? d.getDeliveryPartner().getPhone() : null,
                    d.getStatus(),
                    d.getAssignedAt(),
                    d.getPickedUpAt(),
                    d.getDeliveredAt()
            ));
        }

        return dto;
    }
}
