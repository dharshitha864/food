package com.foodexpress.service;

import com.foodexpress.dto.DeliveryDto;
import com.foodexpress.entity.*;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.DeliveryRepository;
import com.foodexpress.repository.OrderRepository;
import com.foodexpress.repository.PaymentRepository;
import com.foodexpress.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           PaymentRepository paymentRepository,
                           NotificationService notificationService) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    public DeliveryDto assignDeliveryPartner(Long orderId, Long deliveryPartnerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User partner = userRepository.findById(deliveryPartnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        if (partner.getRole() != Role.ROLE_DELIVERY_PARTNER) {
            throw new BadRequestException("Selected user is not a delivery partner");
        }

        Delivery delivery = deliveryRepository.findByOrderId(orderId).orElseGet(() -> {
            Delivery d = new Delivery();
            d.setOrder(order);
            return d;
        });

        delivery.setDeliveryPartner(partner);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);

        order.setStatus(OrderStatus.DELIVERY_PARTNER_ASSIGNED);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getUser().getId(),
                "Delivery Partner Assigned",
                partner.getFullName() + " has been assigned to deliver your Order #" + order.getOrderNumber()
        );

        notificationService.createNotification(
                partner.getId(),
                "New Order Assigned",
                "You have been assigned to deliver Order #" + order.getOrderNumber() + " from " + order.getRestaurant().getName()
        );

        return mapToDto(saved);
    }

    public DeliveryDto markPickedUp(Long orderId, Long partnerId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (!delivery.getDeliveryPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Order not assigned to you");
        }

        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(LocalDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getUser().getId(),
                "Order Picked Up",
                "Your Order #" + order.getOrderNumber() + " has been picked up from the restaurant."
        );

        return mapToDto(saved);
    }

    public DeliveryDto markOutForDelivery(Long orderId, Long partnerId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (!delivery.getDeliveryPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Order not assigned to you");
        }

        delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        Delivery saved = deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getUser().getId(),
                "Out For Delivery",
                "Your Order #" + order.getOrderNumber() + " is out for delivery! Prepare to receive your meal."
        );

        return mapToDto(saved);
    }

    public DeliveryDto markDelivered(Long orderId, Long partnerId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (!delivery.getDeliveryPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Order not assigned to you");
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);

        Order order = delivery.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // If Cash on delivery, mark payment as SUCCESS
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getPaymentMethod() == PaymentMethod.COD && payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                paymentRepository.save(payment);
            }
        });

        notificationService.createNotification(
                order.getUser().getId(),
                "Order Delivered!",
                "Your Order #" + order.getOrderNumber() + " has been successfully delivered. Enjoy your food! Please leave a review."
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> getPartnerDeliveries(Long partnerId) {
        return deliveryRepository.findByDeliveryPartnerIdOrderByAssignedAtDesc(partnerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatus(status)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .map(this::mapToDto)
                .orElse(null);
    }

    public DeliveryDto mapToDto(Delivery d) {
        return new DeliveryDto(
                d.getId(),
                d.getOrder().getId(),
                d.getOrder().getOrderNumber(),
                d.getDeliveryPartner() != null ? d.getDeliveryPartner().getId() : null,
                d.getDeliveryPartner() != null ? d.getDeliveryPartner().getFullName() : null,
                d.getDeliveryPartner() != null ? d.getDeliveryPartner().getPhone() : null,
                d.getStatus(),
                d.getAssignedAt(),
                d.getPickedUpAt(),
                d.getDeliveredAt()
        );
    }
}
