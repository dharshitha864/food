package com.foodexpress.controller;

import com.foodexpress.dto.AddressDto;
import com.foodexpress.dto.ApiResponse;
import com.foodexpress.security.SecurityUtils;
import com.foodexpress.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Customer Addresses", description = "Customer Delivery Address APIs")
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    public AddressController(AddressService addressService, SecurityUtils securityUtils) {
        this.addressService = addressService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get user delivery addresses")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAddresses() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved", addressService.getUserAddresses(userId)));
    }

    @PostMapping
    @Operation(summary = "Add a new delivery address")
    public ResponseEntity<ApiResponse<AddressDto>> addAddress(@Valid @RequestBody AddressDto dto) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added", addressService.addAddress(userId, dto)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery address")
    public ResponseEntity<ApiResponse<AddressDto>> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto dto) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(id, userId, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete delivery address")
    public ResponseEntity<ApiResponse<String>> deleteAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressService.deleteAddress(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<String>> setDefaultAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressService.setDefaultAddress(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated"));
    }
}
