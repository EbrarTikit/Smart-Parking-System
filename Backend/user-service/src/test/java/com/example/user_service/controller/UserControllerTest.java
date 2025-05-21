package com.example.user_service.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.user_service.dto.NotificationPreferencesDto;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;

public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private NotificationPreferencesDto preferencesDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setParkingFullNotification(true);

        preferencesDto = new NotificationPreferencesDto(true);
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User());
        when(userService.findAllUsers()).thenReturn(users);

        // Act
        List<User> result = userController.getAllUsers();

        // Assert
        assertEquals(2, result.size());
        verify(userService).findAllUsers();
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userService.findUserById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<User> response = userController.getUserById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
        verify(userService).findUserById(1L);
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturnNotFound() {
        // Arrange
        when(userService.findUserById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<User> response = userController.getUserById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService).findUserById(999L);
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        when(userService.saveUser(any(User.class))).thenReturn(testUser);

        // Act
        User result = userController.createUser(newUser);

        // Assert
        assertEquals(testUser, result);
        verify(userService).saveUser(any(User.class));
    }

    @Test
    void deleteUser_ShouldReturnOk() {
        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act
        ResponseEntity<Void> response = userController.deleteUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).deleteUser(1L);
    }

    @Test
    void getNotificationPreferences_ShouldReturnPreferences() {
        // Arrange
        when(userService.getNotificationPreferences(1L)).thenReturn(preferencesDto);

        // Act
        ResponseEntity<NotificationPreferencesDto> response = userController.getNotificationPreferences(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(preferencesDto, response.getBody());
        verify(userService).getNotificationPreferences(1L);
    }

    @Test
    void toggleNotificationPreferences_ShouldReturnUpdatedPreferences() {
        // Arrange
        NotificationPreferencesDto updatedPreferences = new NotificationPreferencesDto(false);
        when(userService.toggleNotificationPreferences(1L)).thenReturn(updatedPreferences);

        // Act
        ResponseEntity<NotificationPreferencesDto> response = userController.toggleNotificationPreferences(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedPreferences, response.getBody());
        assertFalse(response.getBody().isParkingFullNotification());
        verify(userService).toggleNotificationPreferences(1L);
    }
}
