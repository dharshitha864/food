package com.foodexpress.controller;

import com.foodexpress.dto.*;
import com.foodexpress.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
@Tag(name = "System Admin", description = "System Administration and Platform Analytics APIs")
public class AdminController {

    private final OrderService orderService;
    private final UserService userService;
    private final RestaurantService restaurantService;
    private final CouponService couponService;
    private final ReviewService reviewService;

    public AdminController(OrderService orderService,
                           UserService userService,
                           RestaurantService restaurantService,
                           CouponService couponService,
                           ReviewService reviewService) {
        this.orderService = orderService;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.couponService = couponService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get platform metrics and statistics")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", orderService.getAdminDashboardStats()));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all registered users")
    public ResponseEntity<ApiResponse<List<UserProfileDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers()));
    }

    @PutMapping("/users/{id}/toggle-status")
    @Operation(summary = "Enable or disable user account")
    public ResponseEntity<ApiResponse<UserProfileDto>> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User status changed", userService.toggleUserStatus(id)));
    }

    @GetMapping("/restaurants")
    @Operation(summary = "Get all restaurants across system")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getAllRestaurants() {
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved", restaurantService.getAllRestaurantsForAdmin()));
    }

    @PutMapping("/restaurants/{id}/toggle-status")
    @Operation(summary = "Approve or toggle restaurant status")
    public ResponseEntity<ApiResponse<RestaurantDto>> toggleRestaurantStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated", restaurantService.toggleRestaurantStatus(id)));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get all platform orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orderService.getAllOrders()));
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get all platform reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getAllReviews() {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviewService.getAllReviews()));
    }

    @GetMapping("/coupons")
    @Operation(summary = "Get all promotional coupons")
    public ResponseEntity<ApiResponse<List<CouponDto>>> getAllCoupons() {
        return ResponseEntity.ok(ApiResponse.success("Coupons retrieved", couponService.getAllCoupons()));
    }

    @PostMapping("/coupons")
    @Operation(summary = "Create new promotional coupon")
    public ResponseEntity<ApiResponse<CouponDto>> createCoupon(@Valid @RequestBody CouponDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", couponService.createCoupon(dto)));
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update promotional coupon")
    public ResponseEntity<ApiResponse<CouponDto>> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", couponService.updateCoupon(id, dto)));
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Delete promotional coupon")
    public ResponseEntity<ApiResponse<String>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted successfully"));
    }
}
