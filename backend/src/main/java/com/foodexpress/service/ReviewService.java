package com.foodexpress.service;

import com.foodexpress.dto.CreateReviewRequest;
import com.foodexpress.dto.ReviewDto;
import com.foodexpress.entity.*;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantService restaurantService;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         RestaurantRepository restaurantRepository,
                         FoodItemRepository foodItemRepository,
                         RestaurantService restaurantService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.restaurantService = restaurantService;
    }

    public ReviewDto createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You cannot review an order placed by another user");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review orders that have been successfully DELIVERED");
        }

        if (reviewRepository.existsByUserIdAndOrderId(userId, order.getId())) {
            throw new BadRequestException("You have already submitted a review for this order");
        }

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        FoodItem foodItem = null;
        if (request.getFoodItemId() != null) {
            foodItem = foodItemRepository.findById(request.getFoodItemId()).orElse(null);
        }

        Review review = new Review(
                null,
                user,
                order,
                restaurant,
                foodItem,
                request.getRating(),
                request.getComment(),
                request.getReviewType() != null ? request.getReviewType() : "RESTAURANT"
        );

        Review saved = reviewRepository.save(review);

        // Update restaurant average rating
        Double avgRating = reviewRepository.calculateAverageRatingByRestaurantId(restaurant.getId());
        if (avgRating != null) {
            restaurantService.updateRating(restaurant.getId(), avgRating);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByRestaurant(Long restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByFoodItem(Long foodItemId) {
        return reviewRepository.findByFoodItemIdOrderByCreatedAtDesc(foodItemId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized: You cannot delete another user's review");
        }

        Long restaurantId = review.getRestaurant().getId();
        reviewRepository.delete(review);

        // Recalculate average rating
        Double avgRating = reviewRepository.calculateAverageRatingByRestaurantId(restaurantId);
        restaurantService.updateRating(restaurantId, avgRating != null ? avgRating : 4.0);
    }

    private ReviewDto mapToDto(Review r) {
        return new ReviewDto(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getOrder().getId(),
                r.getRestaurant().getId(),
                r.getRestaurant().getName(),
                r.getFoodItem() != null ? r.getFoodItem().getId() : null,
                r.getFoodItem() != null ? r.getFoodItem().getName() : null,
                r.getRating(),
                r.getComment(),
                r.getReviewType(),
                r.getCreatedAt()
        );
    }
}
