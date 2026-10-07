package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.FoodItemCreateDto;
import com.foodexpress.dto.FoodItemDto;
import com.foodexpress.service.FoodItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Food Items", description = "Menu and Food Items Management APIs")
public class FoodItemController {

    private final FoodItemService foodItemService;

    public FoodItemController(FoodItemService foodItemService) {
        this.foodItemService = foodItemService;
    }

    // Public APIs
    @GetMapping("/api/foods/{id}")
    @Operation(summary = "Get food item details (Public)")
    public ResponseEntity<ApiResponse<FoodItemDto>> getFoodItemById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Food item retrieved", foodItemService.getFoodItemById(id)));
    }

    @GetMapping("/api/foods/restaurant/{restaurantId}")
    @Operation(summary = "Get menu for restaurant (Public)")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> getFoodsByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant menu retrieved",
                foodItemService.getAvailableFoodItemsByRestaurant(restaurantId)));
    }

    @GetMapping("/api/foods/category/{categoryId}")
    @Operation(summary = "Get food items by category (Public)")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> getFoodsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Category foods retrieved",
                foodItemService.getFoodItemsByCategory(categoryId)));
    }

    @GetMapping("/api/foods/search")
    @Operation(summary = "Search food items", description = "Search food by keyword, category, vegetarian filter, and price")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> searchFoods(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isVeg,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
                foodItemService.searchFoodItems(restaurantId, categoryId, keyword, isVeg, maxPrice, sortBy)));
    }

    // Restaurant Admin APIs
    @PostMapping("/api/restaurant-admin/foods")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Add food item to menu (Restaurant Admin)")
    public ResponseEntity<ApiResponse<FoodItemDto>> createFoodItem(@Valid @RequestBody FoodItemCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Food item added to menu", foodItemService.createFoodItem(dto)));
    }

    @PutMapping("/api/restaurant-admin/foods/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update food item (Restaurant Admin)")
    public ResponseEntity<ApiResponse<FoodItemDto>> updateFoodItem(
            @PathVariable Long id,
            @Valid @RequestBody FoodItemCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Food item updated", foodItemService.updateFoodItem(id, dto)));
    }

    @PatchMapping("/api/restaurant-admin/foods/{id}/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Toggle food availability (Restaurant Admin)")
    public ResponseEntity<ApiResponse<FoodItemDto>> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Availability updated", foodItemService.toggleAvailability(id)));
    }

    @DeleteMapping("/api/restaurant-admin/foods/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Delete food item (Restaurant Admin)")
    public ResponseEntity<ApiResponse<String>> deleteFoodItem(@PathVariable Long id) {
        foodItemService.deleteFoodItem(id);
        return ResponseEntity.ok(ApiResponse.success("Food item deleted successfully"));
    }

    @GetMapping("/api/restaurant-admin/foods/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Get full menu including unavailable items (Restaurant Admin)")
    public ResponseEntity<ApiResponse<List<FoodItemDto>>> getAllFoodsByRestaurantAdmin(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Complete restaurant menu retrieved",
                foodItemService.getFoodItemsByRestaurant(restaurantId)));
    }
}
