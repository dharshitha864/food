package com.foodexpress.controller;

import com.foodexpress.dto.AddToCartRequest;
import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.ApplyCouponRequest;
import com.foodexpress.dto.CartDto;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping Cart and Coupon Application APIs")
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    public CartController(CartService cartService, SecurityUtils securityUtils) {
        this.cartService = cartService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get user shopping cart")
    public ResponseEntity<ApiResponse<CartDto>> getCart() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cartService.getCartDto(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add food item to cart")
    public ResponseEntity<ApiResponse<CartDto>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addToCart(userId, request)));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update quantity of cart item")
    public ResponseEntity<ApiResponse<CartDto>> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Quantity updated", cartService.updateQuantity(userId, cartItemId, quantity)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartDto>> removeItem(@PathVariable Long cartItemId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeItem(userId, cartItemId)));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear all items in cart")
    public ResponseEntity<ApiResponse<CartDto>> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartService.clearCart(userId)));
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Apply discount coupon to cart")
    public ResponseEntity<ApiResponse<CartDto>> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Coupon applied successfully", cartService.applyCoupon(userId, request.getCode())));
    }

    @DeleteMapping("/remove-coupon")
    @Operation(summary = "Remove coupon from cart")
    public ResponseEntity<ApiResponse<CartDto>> removeCoupon() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Coupon removed", cartService.removeCoupon(userId)));
    }
}
