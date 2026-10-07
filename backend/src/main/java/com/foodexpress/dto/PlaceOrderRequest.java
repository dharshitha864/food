package com.foodexpress.dto;

import com.foodexpress.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PlaceOrderRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String couponCode;
    private String specialInstructions;

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(Long addressId, PaymentMethod paymentMethod, String couponCode, String specialInstructions) {
        this.addressId = addressId;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
        this.specialInstructions = specialInstructions;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }
}
