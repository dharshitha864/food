package com.foodexpress.service;

import com.foodexpress.dto.CouponDto;
import com.foodexpress.entity.Coupon;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validCoupon;

    @BeforeEach
    void setUp() {
        validCoupon = new Coupon(
                1L,
                "WELCOME50",
                new BigDecimal("50.0"),
                new BigDecimal("100.00"),
                new BigDecimal("150.00"),
                true,
                LocalDate.now().plusMonths(1)
        );
    }

    @Test
    @DisplayName("Calculate discount correctly with percentage cap")
    void testCalculateDiscount_Success() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("WELCOME50"))
                .thenReturn(Optional.of(validCoupon));

        // Subtotal = 300, 50% = 150, but maxDiscount = 100
        BigDecimal discount = couponService.calculateDiscount("WELCOME50", new BigDecimal("300.00"));

        assertEquals(new BigDecimal("100.00"), discount);
        verify(couponRepository, times(1)).findByCodeIgnoreCaseAndIsActiveTrue("WELCOME50");
    }

    @Test
    @DisplayName("Throw BadRequestException when subtotal is below minimum order value")
    void testCalculateDiscount_BelowMinOrder() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("WELCOME50"))
                .thenReturn(Optional.of(validCoupon));

        assertThrows(BadRequestException.class, () -> {
            couponService.calculateDiscount("WELCOME50", new BigDecimal("100.00"));
        });
    }

    @Test
    @DisplayName("Throw BadRequestException when coupon is expired")
    void testCalculateDiscount_Expired() {
        validCoupon.setValidUntil(LocalDate.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("WELCOME50"))
                .thenReturn(Optional.of(validCoupon));

        assertThrows(BadRequestException.class, () -> {
            couponService.calculateDiscount("WELCOME50", new BigDecimal("200.00"));
        });
    }

    @Test
    @DisplayName("Return zero discount for blank code")
    void testCalculateDiscount_BlankCode() {
        BigDecimal discount = couponService.calculateDiscount("", new BigDecimal("200.00"));
        assertEquals(BigDecimal.ZERO, discount);
    }
}
