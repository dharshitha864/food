package com.foodexpress.service;

import com.foodexpress.dto.FoodItemCreateDto;
import com.foodexpress.dto.FoodItemDto;
import com.foodexpress.entity.FoodCategory;
import com.foodexpress.entity.FoodItem;
import com.foodexpress.entity.Restaurant;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.FoodCategoryRepository;
import com.foodexpress.repository.FoodItemRepository;
import com.foodexpress.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodCategoryRepository categoryRepository;

    public FoodItemService(FoodItemRepository foodItemRepository,
                           RestaurantRepository restaurantRepository,
                           FoodCategoryRepository categoryRepository) {
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> getFoodItemsByRestaurant(Long restaurantId) {
        return foodItemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> getAvailableFoodItemsByRestaurant(Long restaurantId) {
        return foodItemRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> getFoodItemsByCategory(Long categoryId) {
        return foodItemRepository.findByCategoryIdAndIsAvailableTrue(categoryId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodItemDto getFoodItemById(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));
        return mapToDto(item);
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> searchFoodItems(Long restaurantId, Long categoryId, String keyword, Boolean isVeg, BigDecimal maxPrice, String sortBy) {
        String query = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<FoodItem> items = foodItemRepository.searchFoodItems(restaurantId, categoryId, query, isVeg, maxPrice);

        if ("price_asc".equalsIgnoreCase(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getPrice));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getPrice).reversed());
        } else if ("name".equalsIgnoreCase(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getName, String.CASE_INSENSITIVE_ORDER));
        }

        return items.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public FoodItemDto createFoodItem(FoodItemCreateDto dto) {
        Restaurant restaurant = restaurantRepository.findById(dto.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + dto.getRestaurantId()));

        FoodCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        FoodItem item = new FoodItem(
                null,
                restaurant,
                category,
                dto.getName().trim(),
                dto.getDescription(),
                dto.getPrice(),
                dto.isAvailable(),
                dto.isVeg(),
                dto.getImageUrl()
        );

        return mapToDto(foodItemRepository.save(item));
    }

    public FoodItemDto updateFoodItem(Long id, FoodItemCreateDto dto) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));

        FoodCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));

        item.setName(dto.getName().trim());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setAvailable(dto.isAvailable());
        item.setVeg(dto.isVeg());
        item.setCategory(category);
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            item.setImageUrl(dto.getImageUrl());
        }

        return mapToDto(foodItemRepository.save(item));
    }

    public FoodItemDto toggleAvailability(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));
        item.setAvailable(!item.isAvailable());
        return mapToDto(foodItemRepository.save(item));
    }

    public void deleteFoodItem(Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with ID: " + id));
        foodItemRepository.delete(item);
    }

    public FoodItemDto mapToDto(FoodItem item) {
        return new FoodItemDto(
                item.getId(),
                item.getRestaurant() != null ? item.getRestaurant().getId() : null,
                item.getRestaurant() != null ? item.getRestaurant().getName() : null,
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.isAvailable(),
                item.isVeg(),
                item.getImageUrl(),
                item.getCreatedAt()
        );
    }
}
