package com.example.parking_management_service.user_density.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ParkingViewerTest {

    @Test
    void testGettersAndSetters() {
        // Arrange
        ParkingViewer viewer = new ParkingViewer();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(45);
        
        // Act
        viewer.setId(1L);
        viewer.setUserId(2L);
        viewer.setParkingId(3L);
        viewer.setViewingStartTime(now);
        viewer.setExpiryTime(expiry);
        viewer.setNotificationSent(true);
        
        // Assert
        assertEquals(1L, viewer.getId());
        assertEquals(2L, viewer.getUserId());
        assertEquals(3L, viewer.getParkingId());
        assertEquals(now, viewer.getViewingStartTime());
        assertEquals(expiry, viewer.getExpiryTime());
        assertTrue(viewer.isNotificationSent());
    }

    @Test
    void testConstructor() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(45);
        
        // Act
        ParkingViewer viewer = new ParkingViewer(2L, 3L, now, expiry);
        
        // Assert
        assertNull(viewer.getId()); // ID should be null until persisted
        assertEquals(2L, viewer.getUserId());
        assertEquals(3L, viewer.getParkingId());
        assertEquals(now, viewer.getViewingStartTime());
        assertEquals(expiry, viewer.getExpiryTime());
        assertFalse(viewer.isNotificationSent()); // Default should be false
    }
    
    @Test
    void testDefaultConstructor() {
        // Act
        ParkingViewer viewer = new ParkingViewer();
        
        // Assert
        assertNull(viewer.getId());
        assertNull(viewer.getUserId());
        assertNull(viewer.getParkingId());
        assertNull(viewer.getViewingStartTime());
        assertNull(viewer.getExpiryTime());
        assertFalse(viewer.isNotificationSent());
    }
}
