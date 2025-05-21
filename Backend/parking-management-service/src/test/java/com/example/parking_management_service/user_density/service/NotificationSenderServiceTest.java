package com.example.parking_management_service.user_density.service;

import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.user_density.dto.ParkingFullNotificationDTO;
import com.example.parking_management_service.user_density.model.ParkingViewer;
import com.example.parking_management_service.user_density.repository.ParkingViewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationSenderServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private ParkingViewerRepository parkingViewerRepository;

    private NotificationSenderService notificationSenderService;

    private Parking testParking;
    private ParkingViewer testViewer1;
    private ParkingViewer testViewer2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Constructor injection instead of field injection
        notificationSenderService = new NotificationSenderService(
            rabbitTemplate, 
            parkingRepository, 
            parkingViewerRepository
        );
        
        now = LocalDateTime.now();
        
        // Test parking setup
        testParking = mock(Parking.class);
        when(testParking.getId()).thenReturn(1L);
        when(testParking.getName()).thenReturn("Test Parking");
        when(testParking.getCapacity()).thenReturn(5);
        
        // Test viewers setup
        testViewer1 = new ParkingViewer();
        testViewer1.setId(1L);
        testViewer1.setUserId(1L);
        testViewer1.setParkingId(1L);
        testViewer1.setViewingStartTime(now);
        testViewer1.setExpiryTime(now.plusMinutes(45));
        testViewer1.setNotificationSent(false);
        
        testViewer2 = new ParkingViewer();
        testViewer2.setId(2L);
        testViewer2.setUserId(2L);
        testViewer2.setParkingId(1L);
        testViewer2.setViewingStartTime(now);
        testViewer2.setExpiryTime(now.plusMinutes(45));
        testViewer2.setNotificationSent(false);
    }

    @Test
    void sendParkingFullNotification_ShouldSendNotificationsToActiveUsers() {
        // Arrange
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        
        // Create a set of parking spots that are all occupied
        Set<ParkingSpot> fullSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.isAvailable()).thenReturn(false);
            fullSpots.add(spot);
        }
        when(testParking.getParkingSpots()).thenReturn(fullSpots);
        
        when(parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testViewer1, testViewer2));
        
        when(parkingViewerRepository.findById(1L)).thenReturn(Optional.of(testViewer1));
        when(parkingViewerRepository.findById(2L)).thenReturn(Optional.of(testViewer2));
        
        // Act
        notificationSenderService.sendParkingFullNotification(1L);
        
        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
            anyString(),
            anyString(),
            any(ParkingFullNotificationDTO.class)
        );
        
        verify(parkingViewerRepository).save(testViewer1);
        verify(parkingViewerRepository).save(testViewer2);
        assertTrue(testViewer1.isNotificationSent());
        assertTrue(testViewer2.isNotificationSent());
    }

    @Test
    void sendParkingFullNotification_WhenNoUsersToNotify_ShouldNotSendNotifications() {
        // Arrange
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        
        // Create a set of parking spots that are all occupied
        Set<ParkingSpot> fullSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.isAvailable()).thenReturn(false);
            fullSpots.add(spot);
        }
        when(testParking.getParkingSpots()).thenReturn(fullSpots);
        
        when(parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        // Act
        notificationSenderService.sendParkingFullNotification(1L);
        
        // Assert
        verify(rabbitTemplate, never()).convertAndSend(
            anyString(),
            anyString(),
            any(ParkingFullNotificationDTO.class)
        );
        
        verify(parkingViewerRepository, never()).save(any(ParkingViewer.class));
    }

    @Test
    void sendParkingFullNotification_WhenParkingDoesNotExist_ShouldNotSendNotifications() {
        // Arrange
        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        notificationSenderService.sendParkingFullNotification(999L);
        
        // Assert
        verify(rabbitTemplate, never()).convertAndSend(
            anyString(),
            anyString(),
            any(ParkingFullNotificationDTO.class)
        );
        
        verify(parkingViewerRepository, never()).findActiveNonNotifiedViewersByParkingId(anyLong(), any(LocalDateTime.class));
        verify(parkingViewerRepository, never()).save(any(ParkingViewer.class));
    }

    @Test
    void sendParkingFullNotification_WhenRabbitMQFails_ShouldStillMarkUsersAsNotified() {
        // Arrange
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        
        // Create a set of parking spots that are all occupied
        Set<ParkingSpot> fullSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.isAvailable()).thenReturn(false);
            fullSpots.add(spot);
        }
        when(testParking.getParkingSpots()).thenReturn(fullSpots);
        
        when(parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testViewer1, testViewer2));
        
        when(parkingViewerRepository.findById(1L)).thenReturn(Optional.of(testViewer1));
        when(parkingViewerRepository.findById(2L)).thenReturn(Optional.of(testViewer2));
        
        // Simulate RabbitMQ failure
        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(ParkingFullNotificationDTO.class)
            );
        
        // Act - should not throw exception
        notificationSenderService.sendParkingFullNotification(1L);
        
        // Assert - should still mark users as notified
        verify(parkingViewerRepository).save(testViewer1);
        verify(parkingViewerRepository).save(testViewer2);
        assertTrue(testViewer1.isNotificationSent());
        assertTrue(testViewer2.isNotificationSent());
    }
    
    @Test
    void sendParkingFullNotification_WhenParkingNotFull_ShouldNotSendNotifications() {
        // Arrange
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        
        // Create a set of parking spots that are not all occupied
        Set<ParkingSpot> notFullSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.isAvailable()).thenReturn(i < 2); // 2 spots available, 3 occupied
            notFullSpots.add(spot);
        }
        when(testParking.getParkingSpots()).thenReturn(notFullSpots);
        
        // Act
        notificationSenderService.sendParkingFullNotification(1L);
        
        // Assert
        verify(parkingViewerRepository, never()).findActiveNonNotifiedViewersByParkingId(anyLong(), any(LocalDateTime.class));
        verify(rabbitTemplate, never()).convertAndSend(
            anyString(),
            anyString(),
            any(ParkingFullNotificationDTO.class)
        );
    }
}
