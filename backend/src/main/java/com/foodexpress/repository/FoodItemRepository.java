package com.foodexpress.repository;

import com.foodexpress.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByRestaurantId(Long restaurantId);
    List<FoodItem> findByRestaurantIdAndIsAvailableTrue(Long restaurantId);
    List<FoodItem> findByCategoryIdAndIsAvailableTrue(Long categoryId);
    List<FoodItem> findByNameContainingIgnoreCaseAndIsAvailableTrue(String name);

    @Query("SELECT f FROM FoodItem f " +
           "WHERE f.isAvailable = true " +
           "AND (:restaurantId IS NULL OR f.restaurant.id = :restaurantId) " +
           "AND (:categoryId IS NULL OR f.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isVeg IS NULL OR f.isVeg = :isVeg) " +
           "AND (:maxPrice IS NULL OR f.price <= :maxPrice)")
    List<FoodItem> searchFoodItems(
            @Param("restaurantId") Long restaurantId,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("isVeg") Boolean isVeg,
            @Param("maxPrice") BigDecimal maxPrice);
}
