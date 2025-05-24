package com.example.navigation_service.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.exception.ResourceNotFoundException;
import com.example.navigation_service.service.impl.NavigationServiceImpl;

class NavigationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NavigationServiceImpl navigationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the base URL for the service
        ReflectionTestUtils.setField(navigationService, "parkingManagementServiceUrl", 
            "http://parking-management-service:8081");
    }

    @Test
    void getParkingLocationFromParkingService_ShouldReturnParkingLocation() {
        // Arrange
        Long parkingId = 1L;
        ParkingLocationDto expectedDto = new ParkingLocationDto();
        expectedDto.setId(parkingId);
        
        String url = "http://parking-management-service:8081/api/parkings/location/" + parkingId;
        when(restTemplate.getForObject(url, ParkingLocationDto.class)).thenReturn(expectedDto);

        // Act
        ParkingLocationDto result = navigationService.getParkingLocationFromParkingService(parkingId);

        // Assert
        assertNotNull(result);
        assertEquals(parkingId, result.getId());
        verify(restTemplate).getForObject(url, ParkingLocationDto.class);
    }

    @Test
    void getAllParkingLocationFromParkingService_ShouldReturnListOfParkingLocations() {
        // Arrange
        List<ParkingLocationDto> expectedList = Arrays.asList(
            new ParkingLocationDto(),
            new ParkingLocationDto()
        );
        
        String url = "http://parking-management-service:8081/api/parkings/location/list";
        ParameterizedTypeReference<List<ParkingLocationDto>> typeRef = 
            new ParameterizedTypeReference<List<ParkingLocationDto>>() {};
            
        ResponseEntity<List<ParkingLocationDto>> response = 
            new ResponseEntity<>(expectedList, HttpStatus.OK);
            
        when(restTemplate.exchange(
            eq(url),
            eq(HttpMethod.GET),
            isNull(),
            eq(typeRef)
        )).thenReturn(response);

        // Act
        List<ParkingLocationDto> result = navigationService.getAllParkingLocationFromParkingService();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(restTemplate).exchange(
            eq(url),
            eq(HttpMethod.GET),
            isNull(),
            eq(typeRef)
        );
    }

    @Test
    void getParkingLocationFromParkingService_WhenServiceUnavailable_ShouldThrowException() {
        // Arrange
        Long parkingId = 1L;
        String url = "http://parking-management-service:8081/api/parkings/location/" + parkingId;
        when(restTemplate.getForObject(url, ParkingLocationDto.class))
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> 
            navigationService.getParkingLocationFromParkingService(parkingId)
        );
        
        assertEquals("Parking location not found with id: " + parkingId, exception.getMessage());
    }

    @Test
    void getParkingLocationFromParkingService_WhenTimeout_ShouldThrowException() {
        // Arrange
        Long parkingId = 1L;
        String url = "http://parking-management-service:8081/api/parkings/location/" + parkingId;
        when(restTemplate.getForObject(url, ParkingLocationDto.class))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection timed out"));

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> 
            navigationService.getParkingLocationFromParkingService(parkingId)
        );
        assertEquals("Parking location not found with id: " + parkingId, exception.getMessage());
    }

    @Test
    void getAllParkingLocationFromParkingService_WhenInvalidResponse_ShouldThrowException() {
        // Arrange
        String url = "http://parking-management-service:8081/api/parkings/location/list";
        when(restTemplate.exchange(
            eq(url),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new org.springframework.web.client.RestClientException("Invalid JSON response"));

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> 
            navigationService.getAllParkingLocationFromParkingService()
        );
        assertEquals("Error fetching parking locations", exception.getMessage());
    }

    @Test
    void getParkingLocationFromParkingService_WhenEmptyResponse_ShouldThrowException() {
        // Arrange
        Long parkingId = 1L;
        String url = "http://parking-management-service:8081/api/parkings/location/" + parkingId;
        when(restTemplate.getForObject(url, ParkingLocationDto.class))
            .thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> 
            navigationService.getParkingLocationFromParkingService(parkingId)
        );
        assertEquals("Parking location not found with id: " + parkingId, exception.getMessage());
    }

    @Test
    void getParkingLocationFromParkingService_WithNullId_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            navigationService.getParkingLocationFromParkingService(null)
        );
        assertEquals("ID cannot be null", exception.getMessage());
    }
} 