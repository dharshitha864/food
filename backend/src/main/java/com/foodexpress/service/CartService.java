package com.foodexpress.service;

import com.foodexpress.dto.AddToCartRequest;
import com.foodexpress.dto.CartDto;
import com.foodexpress.dto.CartItemDto;
import com.foodexpress.entity.*;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    // Temporary session cache for active coupon per cart ID
    private final java.util.Map<Long, String> cartCouponMap = new java.util.concurrent.ConcurrentHashMap<>();

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       FoodItemRepository foodItemRepository,
                       UserRepository userRepository,
                       CouponService couponService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodItemRepository = foodItemRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
    }

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = new Cart(null, user, null);
            return cartRepository.save(cart);
        });
    }

    @Transactional(readOnly = true)
    public CartDto getCartDto(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartDto(cart);
    }

    public CartDto addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found"));

        if (!foodItem.isAvailable()) {
            throw new BadRequestException("Item '" + foodItem.getName() + "' is currently unavailable");
        }

        // Check if item belongs to a different restaurant than existing cart items
        if (cart.getRestaurant() != null && !cart.getRestaurant().getId().equals(foodItem.getRestaurant().getId())) {
            // Cart contains items from another restaurant - clear old items to start fresh
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
        }

        cart.setRestaurant(foodItem.getRestaurant());

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndFoodItemId(cart.getId(), foodItem.getId());
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            existingItem.setQuantity(newQuantity);
            existingItem.setSubtotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)));
            cartItemRepository.save(existingItem);
        } else {
            BigDecimal subtotal = foodItem.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            CartItem newItem = new CartItem(null, cart, foodItem, request.getQuantity(), foodItem.getPrice(), subtotal);
            cartItemRepository.save(newItem);
        }

        cartRepository.save(cart);
        return buildCartDto(cart);
    }

    public CartDto updateQuantity(Long userId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Unauthorized access to cart item");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            cartItemRepository.save(item);
        }

        return buildCartDto(cart);
    }

    public CartDto removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Unauthorized access to cart item");
        }

        cartItemRepository.delete(item);
        return buildCartDto(cart);
    }

    public CartDto clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setRestaurant(null);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartCouponMap.remove(cart.getId());
        cartRepository.save(cart);
        return buildCartDto(cart);
    }

    public CartDto applyCoupon(Long userId, String code) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Cannot apply coupon to an empty cart");
        }

        BigDecimal subtotal = calculateSubtotal(items);
        // Validates coupon
        couponService.calculateDiscount(code, subtotal);
        cartCouponMap.put(cart.getId(), code.trim().toUpperCase());

        return buildCartDto(cart);
    }

    public CartDto removeCoupon(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartCouponMap.remove(cart.getId());
        return buildCartDto(cart);
    }

    public String getActiveCoupon(Long cartId) {
        return cartCouponMap.get(cartId);
    }

    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartDto buildCartDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        CartDto dto = new CartDto();
        dto.setId(cart.getId());

        if (items.isEmpty()) {
            dto.setRestaurantId(null);
            dto.setRestaurantName(null);
            dto.setItems(new ArrayList<>());
            dto.setSubtotal(BigDecimal.ZERO);
            dto.setDeliveryFee(BigDecimal.ZERO);
            dto.setTaxAmount(BigDecimal.ZERO);
            dto.setDiscountAmount(BigDecimal.ZERO);
            dto.setGrandTotal(BigDecimal.ZERO);
            return dto;
        }

        Restaurant restaurant = items.get(0).getFoodItem().getRestaurant();
        dto.setRestaurantId(restaurant.getId());
        dto.setRestaurantName(restaurant.getName());

        List<CartItemDto> itemDtos = new ArrayList<>();
        for (CartItem item : items) {
            itemDtos.add(new CartItemDto(
                    item.getId(),
                    item.getFoodItem().getId(),
                    item.getFoodItem().getName(),
                    item.getFoodItem().getImageUrl(),
                    item.getFoodItem().isVeg(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()
            ));
        }
        dto.setItems(itemDtos);

        BigDecimal subtotal = calculateSubtotal(items);
        dto.setSubtotal(subtotal);

        BigDecimal deliveryFee = new BigDecimal("40.00");
        dto.setDeliveryFee(deliveryFee);

        // 5% GST tax calculation
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        dto.setTaxAmount(tax);

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = cartCouponMap.get(cart.getId());
        if (couponCode != null) {
            try {
                discount = couponService.calculateDiscount(couponCode, subtotal);
                dto.setCouponCode(couponCode);
            } catch (Exception e) {
                cartCouponMap.remove(cart.getId());
            }
        }
        dto.setDiscountAmount(discount);

        BigDecimal grandTotal = subtotal.add(deliveryFee).add(tax).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }
        dto.setGrandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP));

        cart.setTotalAmount(dto.getGrandTotal());
        cartRepository.save(cart);

        return dto;
    }
}
