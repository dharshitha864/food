package com.foodexpress.dto;

import jakarta.validation.constraints.NotNull;

public class AssignDeliveryRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Delivery partner ID is required")
    private Long deliveryPartnerId;

    public AssignDeliveryRequest() {
    }

    public AssignDeliveryRequest(Long orderId, Long deliveryPartnerId) {
        this.orderId = orderId;
        this.deliveryPartnerId = deliveryPartnerId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getDeliveryPartnerId() {
        return deliveryPartnerId;
    }

    public void setDeliveryPartnerId(Long deliveryPartnerId) {
        this.deliveryPartnerId = deliveryPartnerId;
    }
}
