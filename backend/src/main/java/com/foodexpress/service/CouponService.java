package com.foodexpress.service;

import com.foodexpress.dto.CouponDto;
import com.foodexpress.entity.Coupon;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getActiveCoupons() {
        LocalDate today = LocalDate.now();
        return couponRepository.findByIsActiveTrue()
                .stream()
                .filter(c -> !c.getValidUntil().isBefore(today))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponDto getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        return mapToDto(coupon);
    }

    public CouponDto createCoupon(CouponDto dto) {
        if (couponRepository.findByCodeIgnoreCase(dto.getCode()).isPresent()) {
            throw new BadRequestException("Coupon with code '" + dto.getCode() + "' already exists");
        }

        Coupon coupon = new Coupon(
                null,
                dto.getCode().toUpperCase().trim(),
                dto.getDiscountPercentage(),
                dto.getMaxDiscount(),
                dto.getMinOrderValue() != null ? dto.getMinOrderValue() : BigDecimal.ZERO,
                dto.isActive(),
                dto.getValidUntil()
        );

        return mapToDto(couponRepository.save(coupon));
    }

    public CouponDto updateCoupon(Long id, CouponDto dto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        coupon.setCode(dto.getCode().toUpperCase().trim());
        coupon.setDiscountPercentage(dto.getDiscountPercentage());
        coupon.setMaxDiscount(dto.getMaxDiscount());
        coupon.setMinOrderValue(dto.getMinOrderValue());
        coupon.setActive(dto.isActive());
        coupon.setValidUntil(dto.getValidUntil());

        return mapToDto(couponRepository.save(coupon));
    }

    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        couponRepository.delete(coupon);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or inactive coupon code"));

        if (coupon.getValidUntil().isBefore(LocalDate.now())) {
            throw new BadRequestException("Coupon has expired");
        }

        if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BadRequestException("Order amount must be at least ₹" + coupon.getMinOrderValue() + " to use coupon " + coupon.getCode());
        }

        BigDecimal calculatedDiscount = subtotal.multiply(coupon.getDiscountPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        if (calculatedDiscount.compareTo(coupon.getMaxDiscount()) > 0) {
            return coupon.getMaxDiscount();
        }

        return calculatedDiscount;
    }

    private CouponDto mapToDto(Coupon coupon) {
        return new CouponDto(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountPercentage(),
                coupon.getMaxDiscount(),
                coupon.getMinOrderValue(),
                coupon.isActive(),
                coupon.getValidUntil()
        );
    }
}
