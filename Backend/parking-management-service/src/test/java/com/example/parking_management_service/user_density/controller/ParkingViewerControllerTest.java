package com.example.parking_management_service.user_density.controller;

import com.example.parking_management_service.user_density.dto.ViewerCountDTO;
import com.example.parking_management_service.user_density.service.ParkingViewerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ParkingViewerControllerTest {

    @Mock
    private ParkingViewerService parkingViewerService;

    @InjectMocks
    private ParkingViewerController parkingViewerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void trackUserViewing_ShouldReturnViewerCount() {
        // Arrange
        ViewerCountDTO viewerCount = new ViewerCountDTO(1L, 5L);
        when(parkingViewerService.trackUserViewing(1L, 1L)).thenReturn(viewerCount);
        
        // Act
        ResponseEntity<ViewerCountDTO> response = parkingViewerController.trackUserViewing(1L, 1L);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(viewerCount, response.getBody());
        verify(parkingViewerService).trackUserViewing(1L, 1L);
    }

    @Test
    void getViewerCount_ShouldReturnViewerCount() {
        // Arrange
        ViewerCountDTO viewerCount = new ViewerCountDTO(1L, 5L);
        when(parkingViewerService.getViewerCount(1L)).thenReturn(viewerCount);
        
        // Act
        ResponseEntity<ViewerCountDTO> response = parkingViewerController.getViewerCount(1L);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(viewerCount, response.getBody());
        verify(parkingViewerService).getViewerCount(1L);
    }

    @Test
    void isParkingFull_ShouldReturnParkingStatus() {
        // Arrange
        when(parkingViewerService.isParkingFull(1L)).thenReturn(true);
        
        // Act
        ResponseEntity<Boolean> response = parkingViewerController.isParkingFull(1L);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(parkingViewerService).isParkingFull(1L);
    }

    @Test
    void checkNotifications_ShouldTriggerNotificationCheck() {
        // Act
        ResponseEntity<String> response = parkingViewerController.checkNotifications();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Notification check triggered", response.getBody());
        verify(parkingViewerService).checkParkingStatusForNotifications();
    }
}
