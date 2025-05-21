package com.example.user_service.service;

import com.example.user_service.dto.FcmTokenDto;
import com.example.user_service.model.FcmToken;
import com.example.user_service.model.User;
import com.example.user_service.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    private User testUser;
    private FcmToken testToken;
    private FcmTokenDto tokenDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testToken = new FcmToken();
        testToken.setId(1L);
        testToken.setUserId(1L);
        testToken.setToken("token123");
        testToken.setDeviceId("device123");
        testToken.setActive(true);
        
        tokenDto = new FcmTokenDto("device123", "token123");
    }

    @Test
    void registerToken_WhenNoTokenExists_ShouldCreateNewToken() {
        // Given
        when(fcmTokenRepository.findByTokenAndDeviceId("token123", "device123")).thenReturn(Optional.empty());
        
        // When
        fcmTokenService.registerToken(1L, tokenDto);
        
        // Then
        verify(fcmTokenRepository).findByTokenAndDeviceId("token123", "device123");
        
        ArgumentCaptor<FcmToken> tokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(tokenCaptor.capture());
        
        FcmToken savedToken = tokenCaptor.getValue();
        assertEquals(1L, savedToken.getUserId());
        assertEquals("token123", savedToken.getToken());
        assertEquals("device123", savedToken.getDeviceId());
        assertTrue(savedToken.isActive());
    }

    @Test
    void registerToken_WhenTokenExists_ShouldUpdateExistingToken() {
        // Given
        when(fcmTokenRepository.findByTokenAndDeviceId("token123", "device123")).thenReturn(Optional.of(testToken));
        
        // When
        fcmTokenService.registerToken(2L, tokenDto);
        
        // Then
        verify(fcmTokenRepository).findByTokenAndDeviceId("token123", "device123");
        
        ArgumentCaptor<FcmToken> tokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(tokenCaptor.capture());
        
        FcmToken savedToken = tokenCaptor.getValue();
        assertEquals(2L, savedToken.getUserId());
        assertTrue(savedToken.isActive());
    }

    @Test
    void registerToken_WhenTokenExistsWithSameUserId_ShouldNotUpdate() {
        // Given
        when(fcmTokenRepository.findByTokenAndDeviceId("token123", "device123")).thenReturn(Optional.of(testToken));
        
        // When
        fcmTokenService.registerToken(1L, tokenDto);
        
        // Then
        verify(fcmTokenRepository).findByTokenAndDeviceId("token123", "device123");
        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void deactivateToken_WhenTokenExists_ShouldDeactivateToken() {
        // Given
        List<FcmToken> activeTokens = List.of(testToken);
        when(fcmTokenRepository.findByUserIdAndActiveTrue(1L)).thenReturn(activeTokens);
        
        // When
        fcmTokenService.deactivateToken(1L, "device123");
        
        // Then
        verify(fcmTokenRepository).findByUserIdAndActiveTrue(1L);
        
        ArgumentCaptor<FcmToken> tokenCaptor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(tokenCaptor.capture());
        
        FcmToken savedToken = tokenCaptor.getValue();
        assertFalse(savedToken.isActive());
    }

    @Test
    void deactivateToken_WhenNoTokenExists_ShouldDoNothing() {
        // Given
        when(fcmTokenRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());
        
        // When
        fcmTokenService.deactivateToken(1L, "device123");
        
        // Then
        verify(fcmTokenRepository).findByUserIdAndActiveTrue(1L);
        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void getLatestUserToken_WhenActiveTokenExists_ShouldReturnToken() {
        // Arrange
        FcmToken activeToken = new FcmToken();
        activeToken.setToken("active-token");
        activeToken.setActive(true);
        
        when(fcmTokenRepository.findByUserIdAndActiveTrue(1L)).thenReturn(Arrays.asList(activeToken));

        // Act
        String result = fcmTokenService.getLatestUserToken(1L);

        // Assert
        assertEquals("active-token", result);
        verify(fcmTokenRepository).findByUserIdAndActiveTrue(1L);
    }

    @Test
    void getLatestUserToken_WhenNoActiveTokens_ShouldReturnNull() {
        // Arrange
        when(fcmTokenRepository.findByUserIdAndActiveTrue(1L)).thenReturn(Arrays.asList());

        // Act
        String result = fcmTokenService.getLatestUserToken(1L);

        // Assert
        assertNull(result);
        verify(fcmTokenRepository).findByUserIdAndActiveTrue(1L);
    }
}
