package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.CreateReviewRequest;
import com.foodexpress.dto.ReviewDto;
import com.foodexpress.entity.Role;
import com.foodexpress.entity.User;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Customer Ratings and Written Feedback APIs")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(summary = "Submit a review for completed order")
    public ResponseEntity<ApiResponse<ReviewDto>> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", reviewService.createReview(userId, request)));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get reviews for restaurant (Public)")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getRestaurantReviews(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched",
                reviewService.getReviewsByRestaurant(restaurantId)));
    }

    @GetMapping("/food/{foodItemId}")
    @Operation(summary = "Get reviews for food item (Public)")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getFoodReviews(@PathVariable Long foodItemId) {
        return ResponseEntity.ok(ApiResponse.success("Food reviews fetched",
                reviewService.getReviewsByFoodItem(foodItemId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        boolean isAdmin = user.getRole() == Role.ROLE_SYSTEM_ADMIN;
        reviewService.deleteReview(id, user.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }
}
