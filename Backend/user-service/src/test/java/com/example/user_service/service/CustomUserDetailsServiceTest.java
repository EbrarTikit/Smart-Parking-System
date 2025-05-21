package com.example.user_service.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistent");
        });
        verify(userRepository).findByUsername("nonexistent");
    }
    
    @Test
    void saveUser_ShouldEncodePasswordAndSaveUser() {
        // Arrange
        User userToSave = new User();
        userToSave.setUsername("newuser");
        userToSave.setEmail("new@example.com");
        userToSave.setPassword("rawPassword");
        
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(userToSave);
        
        // Act
        User savedUser = customUserDetailsService.saveUser(userToSave);
        
        // Assert
        assertEquals("encodedPassword", userToSave.getPassword());
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(userToSave);
        assertEquals(savedUser, userToSave);
    }
}