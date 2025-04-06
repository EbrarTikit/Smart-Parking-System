package com.example.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.user_service.dto.JwtResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.SignupRequest;
import com.example.user_service.exception.InvalidCredentialsException;
import com.example.user_service.model.User;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.CustomUserDetailsService;
import com.example.user_service.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("password456");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void authenticateUser_WhenCredentialsAreValid_ShouldReturnToken() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token");
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // When
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof JwtResponse);
        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertEquals("jwt-token", jwtResponse.getToken());
        assertEquals(1L, jwtResponse.getUserId());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("testuser");
        verify(userService).findByUsername("testuser");
    }

    @Test
    void authenticateUser_WhenCredentialsAreInvalid_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When, Then
        assertThrows(InvalidCredentialsException.class, () -> authController.authenticateUser(loginRequest));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void registerUser_WhenUsernameIsUnique_ShouldReturnSuccess() {
        // Given
        when(userService.existsByUsername("newuser")).thenReturn(false);
        
        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setPassword("encodedPassword");
        
        when(userService.registerUser(any(User.class))).thenReturn(savedUser);

        // When
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof String);
        String message = (String) response.getBody();
        assertTrue(message.contains("User registered successfully"));
        assertTrue(message.contains("2"));  // ID of the user

        verify(userService).existsByUsername("newuser");
        verify(userService).registerUser(any(User.class));
    }

    @Test
    void registerUser_WhenUsernameAlreadyExists_ShouldReturnBadRequest() {
        // Given
        when(userService.existsByUsername("newuser")).thenReturn(true);

        // When
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof String);
        String message = (String) response.getBody();
        assertTrue(message.contains("Username is already taken"));

        verify(userService).existsByUsername("newuser");
        verify(userService, never()).registerUser(any(User.class));
    }
}