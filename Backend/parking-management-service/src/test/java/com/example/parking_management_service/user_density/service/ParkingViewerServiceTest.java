package com.example.parking_management_service.user_density.service;

import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.user_density.dto.ViewerCountDTO;
import com.example.parking_management_service.user_density.model.ParkingViewer;
import com.example.parking_management_service.user_density.repository.ParkingViewerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ParkingViewerServiceTest {

    @Mock
    private ParkingViewerRepository parkingViewerRepository;

    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private NotificationSenderService notificationSenderService;

    @InjectMocks
    private ParkingViewerService parkingViewerService;

    private Parking testParking;
    private ParkingViewer testViewer;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        now = LocalDateTime.now();
        
        // Test parking setup
        testParking = mock(Parking.class);
        when(testParking.getId()).thenReturn(1L);
        when(testParking.getName()).thenReturn("Test Parking");
        when(testParking.getCapacity()).thenReturn(10);
        
        Set<ParkingSpot> spots = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.getId()).thenReturn((long) i + 1);
            when(spot.isAvailable()).thenReturn(i < 5); // 5 spots available, 5 occupied
            spots.add(spot);
        }
        when(testParking.getParkingSpots()).thenReturn(spots);
        
        // Test viewer setup
        testViewer = new ParkingViewer();
        testViewer.setId(1L);
        testViewer.setUserId(1L);
        testViewer.setParkingId(1L);
        testViewer.setViewingStartTime(now);
        testViewer.setExpiryTime(now.plusMinutes(45));
        testViewer.setNotificationSent(false);
    }

    @Test
    void trackUserViewing_WhenUserNotAlreadyViewing_ShouldCreateNewRecord() {
        // Arrange
        when(parkingViewerRepository.findByUserIdAndParkingId(1L, 1L)).thenReturn(Optional.empty());
        when(parkingViewerRepository.countActiveViewersByParkingId(eq(1L), any(LocalDateTime.class))).thenReturn(5L);
        
        // Act
        ViewerCountDTO result = parkingViewerService.trackUserViewing(1L, 1L);
        
        // Assert
        ArgumentCaptor<ParkingViewer> viewerCaptor = ArgumentCaptor.forClass(ParkingViewer.class);
        verify(parkingViewerRepository).save(viewerCaptor.capture());
        
        ParkingViewer savedViewer = viewerCaptor.getValue();
        assertEquals(1L, savedViewer.getUserId());
        assertEquals(1L, savedViewer.getParkingId());
        assertFalse(savedViewer.isNotificationSent());
        
        assertEquals(1L, result.getParkingId());
        assertEquals(5L, result.getViewerCount());
    }

    @Test
    void trackUserViewing_WhenUserAlreadyViewing_ShouldUpdateExistingRecord() {
        // Arrange
        when(parkingViewerRepository.findByUserIdAndParkingId(1L, 1L)).thenReturn(Optional.of(testViewer));
        when(parkingViewerRepository.countActiveViewersByParkingId(eq(1L), any(LocalDateTime.class))).thenReturn(5L);
        
        // Act
        ViewerCountDTO result = parkingViewerService.trackUserViewing(1L, 1L);
        
        // Assert
        verify(parkingViewerRepository).save(testViewer);
        assertFalse(testViewer.isNotificationSent());
        
        assertEquals(1L, result.getParkingId());
        assertEquals(5L, result.getViewerCount());
    }

    @Test
    void getViewerCount_ShouldReturnCorrectCount() {
        // Arrange
        when(parkingViewerRepository.countActiveViewersByParkingId(eq(1L), any(LocalDateTime.class))).thenReturn(5L);
        
        // Act
        ViewerCountDTO result = parkingViewerService.getViewerCount(1L);
        
        // Assert
        assertEquals(1L, result.getParkingId());
        assertEquals(5L, result.getViewerCount());
    }

    @Test
    void cleanupExpiredViewers_ShouldDeleteExpiredRecords() {
        // Arrange
        List<ParkingViewer> expiredViewers = Arrays.asList(testViewer);
        when(parkingViewerRepository.findByExpiryTimeLessThan(any(LocalDateTime.class))).thenReturn(expiredViewers);
        
        // Act
        parkingViewerService.cleanupExpiredViewers();
        
        // Assert
        verify(parkingViewerRepository).deleteAll(expiredViewers);
    }

    @Test
    void getUsersToNotifyForFullParking_ShouldReturnCorrectUsers() {
        // Arrange
        List<ParkingViewer> usersToNotify = Arrays.asList(testViewer);
        when(parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(eq(1L), any(LocalDateTime.class)))
            .thenReturn(usersToNotify);
        
        // Act
        List<ParkingViewer> result = parkingViewerService.getUsersToNotifyForFullParking(1L);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(testViewer, result.get(0));
    }

    @Test
    void markUserAsNotified_ShouldUpdateNotificationStatus() {
        // Arrange
        when(parkingViewerRepository.findById(1L)).thenReturn(Optional.of(testViewer));
        
        // Act
        parkingViewerService.markUserAsNotified(1L);
        
        // Assert
        assertTrue(testViewer.isNotificationSent());
        verify(parkingViewerRepository).save(testViewer);
    }

    @Test
    void isParkingFull_WhenParkingExists_ShouldCheckOccupiedSpots() {
        // Arrange
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        
        // Act
        boolean result = parkingViewerService.isParkingFull(1L);
        
        // Assert
        assertFalse(result); // 5 out of 10 spots are occupied, so not full
    }

    @Test
    void isParkingFull_WhenParkingDoesNotExist_ShouldReturnFalse() {
        // Arrange
        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        boolean result = parkingViewerService.isParkingFull(999L);
        
        // Assert
        assertFalse(result);
    }

    @Test
    void checkParkingStatusForNotifications_ShouldSendNotificationsForFullParkings() {
        // Arrange
        Parking fullParking = mock(Parking.class);
        when(fullParking.getId()).thenReturn(2L);
        when(fullParking.getName()).thenReturn("Full Parking");
        when(fullParking.getCapacity()).thenReturn(5);
        
        Set<ParkingSpot> fullSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ParkingSpot spot = mock(ParkingSpot.class);
            when(spot.getId()).thenReturn((long) i + 1);
            when(spot.isAvailable()).thenReturn(false); // All spots occupied
            fullSpots.add(spot);
        }
        when(fullParking.getParkingSpots()).thenReturn(fullSpots);
        
        List<Parking> allParkings = Arrays.asList(testParking, fullParking);
        when(parkingRepository.findAll()).thenReturn(allParkings);
        
        // isParkingFull için mock davranışı
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        when(parkingRepository.findById(2L)).thenReturn(Optional.of(fullParking));
        
        // Act
        parkingViewerService.checkParkingStatusForNotifications();
        
        // Assert
        verify(notificationSenderService).sendParkingFullNotification(2L);
        verify(notificationSenderService, never()).sendParkingFullNotification(1L);
    }
} 