package com.foodexpress.service;

import com.foodexpress.dto.ChangePasswordRequest;
import com.foodexpress.dto.RegisterRequest;
import com.foodexpress.entity.Role;
import com.foodexpress.entity.User;
import com.foodexpress.exception.BadRequestException;
import com.foodexpress.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Test User", "test@example.com", "encoded_pass", "9999999999", Role.ROLE_CUSTOMER, true);
    }

    @Test
    @DisplayName("Register new user successfully")
    void testRegister_Success() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "secret123", "9999999999", Role.ROLE_CUSTOMER);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User created = userService.register(req);

        assertNotNull(created);
        assertEquals("test@example.com", created.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Throw BadRequestException when email already registered")
    void testRegister_DuplicateEmail() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "secret123", "9999999999", Role.ROLE_CUSTOMER);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Change password successfully when current password matches")
    void testChangePassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("old_pass", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.encode("new_pass")).thenReturn("new_encoded_pass");

        ChangePasswordRequest req = new ChangePasswordRequest("old_pass", "new_pass");
        userService.changePassword(1L, req);

        assertEquals("new_encoded_pass", sampleUser.getPassword());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Throw BadRequestException when current password does not match")
    void testChangePassword_InvalidCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest("wrong_pass", "new_pass");

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, req));
        verify(userRepository, never()).save(any(User.class));
    }
}
