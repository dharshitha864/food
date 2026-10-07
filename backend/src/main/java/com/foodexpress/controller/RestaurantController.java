package com.foodexpress.controller;

import com.foodexpress.dto.ApiResponse;
import com.foodexpress.dto.RestaurantCreateDto;
import com.foodexpress.dto.RestaurantDto;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Restaurants", description = "Restaurant Browsing and Management APIs")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final SecurityUtils securityUtils;

    public RestaurantController(RestaurantService restaurantService, SecurityUtils securityUtils) {
        this.restaurantService = restaurantService;
        this.securityUtils = securityUtils;
    }

    // Public APIs
    @GetMapping("/api/restaurants")
    @Operation(summary = "Get all active restaurants (Public)")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getAllActiveRestaurants() {
        return ResponseEntity.ok(ApiResponse.success("Restaurants fetched", restaurantService.getAllActiveRestaurants()));
    }

    @GetMapping("/api/restaurants/{id}")
    @Operation(summary = "Get restaurant by ID (Public)")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant fetched", restaurantService.getRestaurantById(id)));
    }

    @GetMapping("/api/restaurants/search")
    @Operation(summary = "Search and filter restaurants", description = "Filter by name, food keyword, category, minRating, and sort")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> searchRestaurants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "rating") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
                restaurantService.searchRestaurants(keyword, categoryId, minRating, sortBy)));
    }

    // Restaurant Admin APIs
    @PostMapping("/api/restaurant-admin/restaurants")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Register restaurant (Restaurant Admin)")
    public ResponseEntity<ApiResponse<RestaurantDto>> registerRestaurant(@Valid @RequestBody RestaurantCreateDto dto) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant registered successfully", restaurantService.createRestaurant(ownerId, dto)));
    }

    @PutMapping("/api/restaurant-admin/restaurants/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update restaurant details (Restaurant Admin)")
    public ResponseEntity<ApiResponse<RestaurantDto>> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantCreateDto dto) {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.updateRestaurant(id, ownerId, dto)));
    }

    @GetMapping("/api/restaurant-admin/restaurants/my-restaurants")
    @PreAuthorize("hasAnyAuthority('ROLE_RESTAURANT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Get restaurants owned by current user")
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getMyRestaurants() {
        Long ownerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("My restaurants fetched", restaurantService.getRestaurantsByOwner(ownerId)));
    }
}
