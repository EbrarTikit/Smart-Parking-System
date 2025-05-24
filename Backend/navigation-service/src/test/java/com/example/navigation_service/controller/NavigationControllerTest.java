package com.example.navigation_service.controller;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.service.INavigationService;
import com.example.navigation_service.controller.impl.NavigationControllerImpl;
import com.example.navigation_service.exception.ResourceNotFoundException;

class NavigationControllerTest {

    @Mock
    private INavigationService navigationService;

    @InjectMocks
    private NavigationControllerImpl navigationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getParkingLocationFromParkingService_ShouldReturnParkingLocation() {
        // Arrange
        Long parkingId = 1L;
        ParkingLocationDto expectedDto = new ParkingLocationDto();
        expectedDto.setId(parkingId);
        when(navigationService.getParkingLocationFromParkingService(parkingId)).thenReturn(expectedDto);

        // Act
        ParkingLocationDto result = navigationController.getParkingLocationFromParkingService(parkingId);

        // Assert
        assertNotNull(result);
        assertEquals(parkingId, result.getId());
        verify(navigationService).getParkingLocationFromParkingService(parkingId);
    }

    @Test
    void getAllParkingLocationFromParkingService_ShouldReturnListOfParkingLocations() {
        // Arrange
        List<ParkingLocationDto> expectedList = Arrays.asList(
            new ParkingLocationDto(),
            new ParkingLocationDto()
        );
        when(navigationService.getAllParkingLocationFromParkingService()).thenReturn(expectedList);

        // Act
        List<ParkingLocationDto> result = navigationController.getAllParkingLocationFromParkingService();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(navigationService).getAllParkingLocationFromParkingService();
    }

    @Test
    void getParkingLocationFromParkingService_WithInvalidId_ShouldThrowException() {
        // Arrange
        Long invalidId = -1L;
        when(navigationService.getParkingLocationFromParkingService(invalidId))
            .thenThrow(new ResourceNotFoundException("Invalid parking ID"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            navigationController.getParkingLocationFromParkingService(invalidId)
        );
    }

    @Test
    void getAllParkingLocationFromParkingService_WhenEmpty_ShouldReturnEmptyList() {
        // Arrange
        when(navigationService.getAllParkingLocationFromParkingService())
            .thenReturn(Collections.emptyList());

        // Act
        List<ParkingLocationDto> result = navigationController.getAllParkingLocationFromParkingService();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}