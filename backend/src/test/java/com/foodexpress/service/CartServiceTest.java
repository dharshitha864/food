package com.foodexpress.service;

import com.foodexpress.dto.AddToCartRequest;
import com.foodexpress.dto.CartDto;
import com.foodexpress.entity.*;
import com.foodexpress.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private FoodItemRepository foodItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Restaurant restaurant;
    private FoodItem foodItem;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Test User", "test@example.com", "pass", "9999999999", Role.ROLE_CUSTOMER, true);
        restaurant = new Restaurant(10L, user, "Test Restaurant", "Desc", "Address", "12345", "test@rest.com", 4.5, true, null);
        foodItem = new FoodItem(100L, restaurant, null, "Burger", "Juicy", new BigDecimal("150.00"), true, true, null);
        cart = new Cart(5L, user, restaurant);
    }

    @Test
    @DisplayName("Successfully add item to cart and calculate correct grand total")
    void testAddToCart_Success() {
        AddToCartRequest req = new AddToCartRequest(100L, 2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(foodItemRepository.findById(100L)).thenReturn(Optional.of(foodItem));
        when(cartItemRepository.findByCartIdAndFoodItemId(5L, 100L)).thenReturn(Optional.empty());

        CartItem savedItem = new CartItem(1L, cart, foodItem, 2, new BigDecimal("150.00"), new BigDecimal("300.00"));
        when(cartItemRepository.findByCartId(5L)).thenReturn(Collections.singletonList(savedItem));

        CartDto dto = cartService.addToCart(1L, req);

        assertNotNull(dto);
        assertEquals(new BigDecimal("300.00"), dto.getSubtotal());
        assertEquals(new BigDecimal("40.00"), dto.getDeliveryFee());
        // 5% of 300 = 15.00
        assertEquals(new BigDecimal("15.00"), dto.getTaxAmount());
        // 300 + 40 + 15 = 355.00
        assertEquals(new BigDecimal("355.00"), dto.getGrandTotal());
    }

    @Test
    @DisplayName("Clear cart flushes all items and resets totals")
    void testClearCart_Success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartDto dto = cartService.clearCart(1L);

        verify(cartItemRepository).deleteByCartId(5L);
        assertEquals(BigDecimal.ZERO, dto.getGrandTotal());
        assertEquals(0, dto.getItems().size());
    }
}
