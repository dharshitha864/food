package com.foodexpress.service;

import com.foodexpress.dto.FoodCategoryDto;
import com.foodexpress.entity.FoodCategory;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.FoodCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FoodCategoryService {

    private final FoodCategoryRepository categoryRepository;

    public FoodCategoryService(FoodCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodCategoryDto getCategoryById(Long id) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToDto(category);
    }

    public FoodCategoryDto createCategory(FoodCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Category with name '" + dto.getName() + "' already exists");
        }

        FoodCategory category = new FoodCategory(
                null,
                dto.getName().trim(),
                dto.getDescription(),
                dto.getImageUrl()
        );

        return mapToDto(categoryRepository.save(category));
    }

    public FoodCategoryDto updateCategory(Long id, FoodCategoryDto dto) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());

        return mapToDto(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        categoryRepository.delete(category);
    }

    private FoodCategoryDto mapToDto(FoodCategory c) {
        return new FoodCategoryDto(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getImageUrl()
        );
    }
}
