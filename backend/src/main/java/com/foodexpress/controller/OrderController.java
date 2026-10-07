package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.OrderDto;
import com.foodexpress.dto.PlaceOrderRequest;
import com.foodexpress.dto.UpdateOrderStatusRequest;
import com.foodexpress.entity.Role;
import com.foodexpress.entity.User;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order Lifecycle, Placement, and Tracking APIs")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderController(OrderService orderService, SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(summary = "Place a new order from active cart")
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", orderService.placeOrder(userId, request)));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get order history for logged-in customer")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyOrders() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Order history retrieved", orderService.getCustomerOrders(userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved", orderService.getOrderById(id)));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Track order status by order number")
    public ResponseEntity<ApiResponse<OrderDto>> trackOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Tracking information retrieved",
                orderService.getOrderByOrderNumber(orderNumber)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order if not already in preparation")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        boolean isAdmin = user.getRole() == Role.ROLE_SYSTEM_ADMIN;
        return ResponseEntity.ok(ApiResponse.success("Order cancelled",
                orderService.cancelOrder(id, user.getId(), isAdmin)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update order status (Restaurant Admin / System Admin)")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateOrderStatus(id, request.getStatus())));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Get orders received by restaurant")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getRestaurantOrders(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant orders fetched",
                orderService.getRestaurantOrders(restaurantId)));
    }
}
