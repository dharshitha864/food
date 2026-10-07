package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.AssignDeliveryRequest;
import com.foodexpress.dto.DeliveryDto;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Delivery", description = "Delivery Partner and Dispatch APIs")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final SecurityUtils securityUtils;

    public DeliveryController(DeliveryService deliveryService, SecurityUtils securityUtils) {
        this.deliveryService = deliveryService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/api/delivery-partner/assigned")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY_PARTNER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Get assigned orders for current delivery partner")
    public ResponseEntity<ApiResponse<List<DeliveryDto>>> getAssignedDeliveries() {
        Long partnerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Assigned deliveries fetched",
                deliveryService.getPartnerDeliveries(partnerId)));
    }

    @PutMapping("/api/delivery-partner/orders/{orderId}/pickup")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY_PARTNER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Mark order as picked up by courier")
    public ResponseEntity<ApiResponse<DeliveryDto>> markPickedUp(@PathVariable Long orderId) {
        Long partnerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Order marked as picked up",
                deliveryService.markPickedUp(orderId, partnerId)));
    }

    @PutMapping("/api/delivery-partner/orders/{orderId}/out-for-delivery")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY_PARTNER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Mark order as out for delivery")
    public ResponseEntity<ApiResponse<DeliveryDto>> markOutForDelivery(@PathVariable Long orderId) {
        Long partnerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Order is now out for delivery",
                deliveryService.markOutForDelivery(orderId, partnerId)));
    }

    @PutMapping("/api/delivery-partner/orders/{orderId}/deliver")
    @PreAuthorize("hasAnyAuthority('ROLE_DELIVERY_PARTNER', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Mark order as delivered")
    public ResponseEntity<ApiResponse<DeliveryDto>> markDelivered(@PathVariable Long orderId) {
        Long partnerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered",
                deliveryService.markDelivered(orderId, partnerId)));
    }

    @PostMapping("/api/deliveries/assign")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Assign delivery partner to an order (Admin)")
    public ResponseEntity<ApiResponse<DeliveryDto>> assignDeliveryPartner(@Valid @RequestBody AssignDeliveryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Delivery partner assigned",
                deliveryService.assignDeliveryPartner(request.getOrderId(), request.getDeliveryPartnerId())));
    }

    @GetMapping("/api/deliveries/order/{orderId}")
    @Operation(summary = "Get delivery status for order")
    public ResponseEntity<ApiResponse<DeliveryDto>> getDeliveryByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Delivery details retrieved",
                deliveryService.getDeliveryByOrderId(orderId)));
    }
}
