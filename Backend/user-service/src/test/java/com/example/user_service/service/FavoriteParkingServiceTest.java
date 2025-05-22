package com.example.user_service.service;

import com.example.user_service.dto.FavoriteParkingResponseDto;
import com.example.user_service.dto.ParkingDetailsDto;
import com.example.user_service.exception.ParkingServiceException;
import com.example.user_service.model.FavoriteParking;
import com.example.user_service.model.User;
import com.example.user_service.repository.FavoriteParkingRepository;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteParkingServiceTest {

    @Mock
    private FavoriteParkingRepository favoriteParkingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavoriteParkingService favoriteParkingService;

    private User testUser;
    private FavoriteParking testFavoriteParking;
    private ParkingDetailsDto testParkingDetails;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup test favorite parking
        testFavoriteParking = new FavoriteParking();
        testFavoriteParking.setId(1L);
        testFavoriteParking.setUser(testUser);
        testFavoriteParking.setParkingId(101L);

        // Setup test parking details from external service
        testParkingDetails = new ParkingDetailsDto();
        testParkingDetails.setId(101L);
        testParkingDetails.setName("Test Parking");
        testParkingDetails.setLocation("Test Location");
        testParkingDetails.setImageUrl("http://example.com/image.jpg");
        testParkingDetails.setRate(4.5);
    }

    @Test
    void getFavorites_ShouldReturnListOfFavorites() {
        // Arrange
        when(favoriteParkingRepository.findByUserId(1L)).thenReturn(Arrays.asList(testFavoriteParking));
        when(restTemplate.getForObject(anyString(), eq(ParkingDetailsDto.class))).thenReturn(testParkingDetails);

        // Act
        List<FavoriteParkingResponseDto> result = favoriteParkingService.getFavorites(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals("Test Parking", result.get(0).getName());
        assertEquals("Test Location", result.get(0).getLocation());
        assertEquals("http://example.com/image.jpg", result.get(0).getImageUrl());
        assertEquals(4.5, result.get(0).getRate());
        
        verify(favoriteParkingRepository).findByUserId(1L);
        verify(restTemplate).getForObject(anyString(), eq(ParkingDetailsDto.class));
    }

    @Test
    void addToFavorites_ShouldAddParkingToFavorites() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(favoriteParkingRepository.existsByUserIdAndParkingId(1L, 101L)).thenReturn(false);
        when(restTemplate.getForEntity(anyString(), eq(ParkingDetailsDto.class)))
            .thenReturn(ResponseEntity.ok(testParkingDetails));
        when(favoriteParkingRepository.save(any(FavoriteParking.class))).thenReturn(testFavoriteParking);

        // Act
        favoriteParkingService.addToFavorites(1L, 101L);

        // Assert
        verify(userRepository).findById(1L);
        verify(restTemplate).getForEntity(anyString(), eq(ParkingDetailsDto.class));
        verify(favoriteParkingRepository).existsByUserIdAndParkingId(1L, 101L);
        verify(favoriteParkingRepository).save(any(FavoriteParking.class));
    }

    @Test
    void addToFavorites_WhenAlreadyExists_ShouldNotAddAgain() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(restTemplate.getForEntity(anyString(), eq(ParkingDetailsDto.class)))
            .thenReturn(ResponseEntity.ok(testParkingDetails));
        when(favoriteParkingRepository.existsByUserIdAndParkingId(1L, 101L)).thenReturn(true);

        // Act
        favoriteParkingService.addToFavorites(1L, 101L);

        // Assert
        verify(userRepository).findById(1L);
        verify(restTemplate).getForEntity(anyString(), eq(ParkingDetailsDto.class));
        verify(favoriteParkingRepository).existsByUserIdAndParkingId(1L, 101L);
        verify(favoriteParkingRepository, never()).save(any(FavoriteParking.class));
    }

    @Test
    void addToFavorites_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            favoriteParkingService.addToFavorites(999L, 101L);
        });
        
        verify(userRepository).findById(999L);
        verifyNoInteractions(restTemplate);
        verifyNoInteractions(favoriteParkingRepository);
    }

    @Test
    void addToFavorites_WhenParkingServiceFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(restTemplate.getForEntity(anyString(), eq(ParkingDetailsDto.class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // Act & Assert
        assertThrows(ParkingServiceException.class, () -> {
            favoriteParkingService.addToFavorites(1L, 101L);
        });
        
        verify(userRepository).findById(1L);
        verify(restTemplate).getForEntity(anyString(), eq(ParkingDetailsDto.class));
        verifyNoInteractions(favoriteParkingRepository);
    }

    @Test
    void removeFromFavorites_ShouldRemoveParkingFromFavorites() {
        // Arrange
        when(favoriteParkingRepository.findByUserIdAndParkingId(1L, 101L))
            .thenReturn(Optional.of(testFavoriteParking));
        
        // Act
        favoriteParkingService.removeFromFavorites(1L, 101L);
        
        // Assert
        verify(favoriteParkingRepository).findByUserIdAndParkingId(1L, 101L);
        verify(favoriteParkingRepository).delete(testFavoriteParking);
    }

    @Test
    void removeFromFavorites_WhenNotFound_ShouldDoNothing() {
        // Arrange
        when(favoriteParkingRepository.findByUserIdAndParkingId(999L, 101L))
            .thenReturn(Optional.empty());
        
        // Act
        favoriteParkingService.removeFromFavorites(999L, 101L);
        
        // Assert
        verify(favoriteParkingRepository).findByUserIdAndParkingId(999L, 101L);
        verify(favoriteParkingRepository, never()).delete(any());
    }
}
