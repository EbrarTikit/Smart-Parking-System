package com.example.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.user_service.dto.JwtResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.SignupRequest;
import com.example.user_service.exception.InvalidCredentialsException;
import com.example.user_service.model.User;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.CustomUserDetailsService;
import com.example.user_service.service.UserService;

public class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("password");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token");
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
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
    void authenticateUser_WithInvalidCredentials_ShouldThrowException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authController.authenticateUser(loginRequest);
        });
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userService);
    }

    @Test
    void registerUser_WithNewUsername_ShouldRegisterUser() {
        // Arrange
        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.registerUser(any(User.class))).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertTrue(messageResponse.getMessage().contains("User registered successfully"));
        
        verify(userService).existsByUsername("newuser");
        verify(userService).registerUser(any(User.class));
    }

    @Test
    void registerUser_WithExistingUsername_ShouldReturnError() {
        // Arrange
        when(userService.existsByUsername("newuser")).thenReturn(true);

        // Act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertTrue(messageResponse.getMessage().contains("Username is already taken"));
        
        verify(userService).existsByUsername("newuser");
        verify(userService, never()).registerUser(any(User.class));
    }
}
