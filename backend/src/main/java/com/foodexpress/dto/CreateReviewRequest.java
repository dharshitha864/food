package com.foodexpress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateReviewRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    private Long foodItemId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    private String comment;
    private String reviewType = "RESTAURANT";

    public CreateReviewRequest() {
    }

    public CreateReviewRequest(Long orderId, Long restaurantId, Long foodItemId, Integer rating, String comment, String reviewType) {
        this.orderId = orderId;
        this.restaurantId = restaurantId;
        this.foodItemId = foodItemId;
        this.rating = rating;
        this.comment = comment;
        this.reviewType = reviewType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getFoodItemId() {
        return foodItemId;
    }

    public void setFoodItemId(Long foodItemId) {
        this.foodItemId = foodItemId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }
}
