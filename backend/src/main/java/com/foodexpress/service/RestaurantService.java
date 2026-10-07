package com.foodexpress.service;

import com.foodexpress.dto.RestaurantCreateDto;
import com.foodexpress.dto.RestaurantDto;
import com.foodexpress.entity.Restaurant;
import com.foodexpress.entity.User;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.RestaurantRepository;
import com.foodexpress.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> getAllActiveRestaurants() {
        return restaurantRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> getAllRestaurantsForAdmin() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + id));
        return mapToDto(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> getRestaurantsByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> searchRestaurants(String keyword, Long categoryId, Double minRating, String sortBy) {
        String query = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<Restaurant> results = restaurantRepository.searchRestaurants(query, categoryId, minRating);

        if ("rating".equalsIgnoreCase(sortBy)) {
            results.sort(Comparator.comparing(Restaurant::getRating, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("name".equalsIgnoreCase(sortBy)) {
            results.sort(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER));
        }

        return results.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public RestaurantDto createRestaurant(Long ownerId, RestaurantCreateDto dto) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with ID: " + ownerId));

        Restaurant restaurant = new Restaurant(
                null,
                owner,
                dto.getName().trim(),
                dto.getDescription(),
                dto.getAddress(),
                dto.getPhone(),
                dto.getEmail(),
                4.5,
                true,
                dto.getImageUrl()
        );

        return mapToDto(restaurantRepository.save(restaurant));
    }

    public RestaurantDto updateRestaurant(Long id, Long ownerId, RestaurantCreateDto dto) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + id));

        if (ownerId != null && (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId))) {
            throw new BadRequestException("You are not authorized to update this restaurant");
        }

        restaurant.setName(dto.getName().trim());
        restaurant.setDescription(dto.getDescription());
        restaurant.setAddress(dto.getAddress());
        restaurant.setPhone(dto.getPhone());
        restaurant.setEmail(dto.getEmail());
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            restaurant.setImageUrl(dto.getImageUrl());
        }

        return mapToDto(restaurantRepository.save(restaurant));
    }

    public RestaurantDto toggleRestaurantStatus(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + id));
        restaurant.setActive(!restaurant.isActive());
        return mapToDto(restaurantRepository.save(restaurant));
    }

    public void updateRating(Long id, Double newRating) {
        restaurantRepository.findById(id).ifPresent(r -> {
            r.setRating(Math.round(newRating * 10.0) / 10.0);
            restaurantRepository.save(r);
        });
    }

    public RestaurantDto mapToDto(Restaurant r) {
        return new RestaurantDto(
                r.getId(),
                r.getOwner() != null ? r.getOwner().getId() : null,
                r.getOwner() != null ? r.getOwner().getFullName() : null,
                r.getName(),
                r.getDescription(),
                r.getAddress(),
                r.getPhone(),
                r.getEmail(),
                r.getRating(),
                r.isActive(),
                r.getImageUrl(),
                r.getCreatedAt()
        );
    }
}
