package com.foodexpress.service;

import com.foodexpress.dto.AddressDto;
import com.foodexpress.entity.Address;
import com.foodexpress.entity.User;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.AddressRepository;
import com.foodexpress.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressDto getAddressById(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return mapToDto(address);
    }

    public AddressDto addAddress(Long userId, AddressDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(a -> {
                a.setDefault(false);
                addressRepository.save(a);
            });
        } else {
            // If user has no existing addresses, make this one default
            List<Address> existing = addressRepository.findByUserId(userId);
            if (existing.isEmpty()) {
                dto.setDefault(true);
            }
        }

        Address address = new Address(
                null,
                user,
                dto.getStreet(),
                dto.getCity(),
                dto.getState(),
                dto.getPostalCode(),
                dto.isDefault()
        );

        Address saved = addressRepository.save(address);
        return mapToDto(saved);
    }

    public AddressDto updateAddress(Long addressId, Long userId, AddressDto dto) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (dto.isDefault() && !address.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(a -> {
                a.setDefault(false);
                addressRepository.save(a);
            });
        }

        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setDefault(dto.isDefault());

        return mapToDto(addressRepository.save(address));
    }

    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    public void setDefaultAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(a -> {
            a.setDefault(false);
            addressRepository.save(a);
        });

        address.setDefault(true);
        addressRepository.save(address);
    }

    private AddressDto mapToDto(Address address) {
        return new AddressDto(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.isDefault()
        );
    }
}
