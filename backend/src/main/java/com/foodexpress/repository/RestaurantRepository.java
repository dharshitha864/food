package com.foodexpress.repository;

import com.foodexpress.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByIsActiveTrue();
    List<Restaurant> findByOwnerId(Long ownerId);
    Optional<Restaurant> findByIdAndIsActiveTrue(Long id);
    List<Restaurant> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    @Query("SELECT DISTINCT r FROM Restaurant r " +
           "LEFT JOIN FoodItem f ON f.restaurant.id = r.id " +
           "LEFT JOIN FoodCategory c ON f.category.id = c.id " +
           "WHERE r.isActive = true " +
           "AND (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:minRating IS NULL OR r.rating >= :minRating)")
    List<Restaurant> searchRestaurants(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minRating") Double minRating);
}
