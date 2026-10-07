package com.foodexpress.service;

import com.foodexpress.dto.*;
import com.foodexpress.entity.Role;
import com.foodexpress.entity.User;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.exception.ResourceNotFoundException;
import com.foodexpress.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("An account with email '" + request.getEmail() + "' already exists");
        }

        User user = new User(
                null,
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone().trim(),
                request.getRole() != null ? request.getRole() : Role.ROLE_CUSTOMER,
                true
        );

        User saved = userRepository.save(user);

        notificationService.createNotification(
                saved.getId(),
                "Welcome to FoodExpress!",
                "Welcome to FoodExpress, " + saved.getFullName() + "! Start browsing delicious food near you."
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long id) {
        User user = findById(id);
        return mapToDto(user);
    }

    public UserProfileDto updateProfile(Long id, UserProfileDto dto) {
        User user = findById(id);

        if (!user.getEmail().equalsIgnoreCase(dto.getEmail().trim())) {
            if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
                throw new BadRequestException("Email is already taken by another user");
            }
            user.setEmail(dto.getEmail().trim().toLowerCase());
        }

        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone().trim());

        return mapToDto(userRepository.save(user));
    }

    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = findById(id);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        notificationService.createNotification(
                user.getId(),
                "Password Changed",
                "Your password has been changed successfully. If this wasn't you, contact support immediately."
        );
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));

        notificationService.createNotification(
                user.getId(),
                "Password Reset Requested",
                "A password reset request was initiated for your account. You can reset your password using the reset screen."
        );
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No user registered with email: " + request.getEmail()));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        notificationService.createNotification(
                user.getId(),
                "Password Reset Successful",
                "Your password has been successfully reset. You may now log in with your new password."
        );
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public UserProfileDto toggleUserStatus(Long id) {
        User user = findById(id);
        user.setActive(!user.isActive());
        return mapToDto(userRepository.save(user));
    }

    public UserProfileDto mapToDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive()
        );
    }
}
