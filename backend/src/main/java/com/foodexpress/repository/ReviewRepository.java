package com.foodexpress.repository;

import com.foodexpress.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<Review> findByFoodItemIdOrderByCreatedAtDesc(Long foodItemId);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndOrderId(Long userId, Long orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double calculateAverageRatingByRestaurantId(@Param("restaurantId") Long restaurantId);
}
