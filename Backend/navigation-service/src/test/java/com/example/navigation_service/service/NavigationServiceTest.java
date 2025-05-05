package com.example.navigation_service.service;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.repository.INavigationRepository;
import com.example.navigation_service.service.impl.NavigationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NavigationServiceTest {

    @Mock
    private INavigationRepository carParkRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NavigationServiceImpl navigationService;

    private final String SERVICE_URL = "http://parking-management-service:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(navigationService, "parkingManagementServiceUrl", SERVICE_URL);
    }

    @Test
    void getParkingLocationFromParkingService_Success() {
        // Arrange
        Long id = 1L;
        ParkingLocationDto expectedDto = new ParkingLocationDto(id, "Test Parking", 40.123, 29.456);
        ResponseEntity<ParkingLocationDto> responseEntity = new ResponseEntity<>(expectedDto, HttpStatus.OK);
        
        when(restTemplate.getForEntity(
                eq(SERVICE_URL + "/api/parkings/location/" + id),
                eq(ParkingLocationDto.class)))
                .thenReturn(responseEntity);

        // Act
        ParkingLocationDto result = navigationService.getParkingLocationFromParkingService(id);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
    }

    @Test
    void getParkingLocationFromParkingService_ReturnsNull_WhenExceptionOccurs() {
        // Arrange
        Long id = 1L;
        when(restTemplate.getForEntity(anyString(), eq(ParkingLocationDto.class)))
                .thenThrow(new RuntimeException("Connection error"));

        // Act
        ParkingLocationDto result = navigationService.getParkingLocationFromParkingService(id);

        // Assert
        assertNull(result);
    }
}