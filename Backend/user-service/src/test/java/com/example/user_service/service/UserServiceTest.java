package com.example.user_service.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.user_service.dto.NotificationPreferencesDto;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setParkingFullNotification(true);
    }
    
    @Test
    void findAllUsers_ShouldReturnAllUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("testuser2");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));
        
        // Act
        List<User> result = userService.findAllUsers();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("testuser2", result.get(1).getUsername());
    }
    
    @Test
    void findUserById_WithValidId_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        Optional<User> result = userService.findUserById(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }
    
    @Test
    void findUserById_WithInvalidId_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act
        Optional<User> result = userService.findUserById(99L);
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void saveUser_ShouldReturnSavedUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        User result = userService.saveUser(testUser);
        
        // Assert
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
    }
    
    @Test
    void deleteUser_ShouldCallRepositoryDelete() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById(1L);
        
        // Act
        userService.deleteUser(1L);
        
        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void deleteUser_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(99L);
        });
        
        assertEquals("User not found with id: 99", exception.getMessage());
    }
    
    @Test
    void findByUsername_WithValidUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        User result = userService.findByUsername("testuser");
        
        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
    
    @Test
    void findByUsername_WithInvalidUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername("nonexistent");
        });
    }
    
    @Test
    void existsByUsername_WithExistingUsername_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        boolean result = userService.existsByUsername("testuser");
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void existsByUsername_WithNonExistingUsername_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // Act
        boolean result = userService.existsByUsername("nonexistent");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void getNotificationPreferences_ShouldReturnPreferences() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        NotificationPreferencesDto result = userService.getNotificationPreferences(1L);
        
        // Assert
        assertTrue(result.isParkingFullNotification());
    }
    
    @Test
    void toggleNotificationPreferences_ShouldToggleAndReturnPreferences() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // Act
        NotificationPreferencesDto result = userService.toggleNotificationPreferences(1L);
        
        // Assert
        assertFalse(result.isParkingFullNotification());
        
        // Toggle back
        result = userService.toggleNotificationPreferences(1L);
        assertTrue(result.isParkingFullNotification());
    }
}
