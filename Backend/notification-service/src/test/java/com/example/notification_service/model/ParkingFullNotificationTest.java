package com.example.notification_service.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ParkingFullNotificationTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        Long parkingId = 1L;
        String parkingName = "Test Parking";
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        // Act
        ParkingFullNotification notification = new ParkingFullNotification(parkingId, parkingName, userIds);
        
        // Assert
        assertEquals(parkingId, notification.getParkingId());
        assertEquals(parkingName, notification.getParkingName());
        assertEquals(userIds, notification.getUserIds());
    }

    @Test
    void testDefaultConstructor() {
        // Act
        ParkingFullNotification notification = new ParkingFullNotification();
        
        // Assert
        assertNull(notification.getParkingId());
        assertNull(notification.getParkingName());
        assertNull(notification.getUserIds());
    }

    @Test
    void testSetters() {
        // Arrange
        ParkingFullNotification notification = new ParkingFullNotification();
        Long parkingId = 1L;
        String parkingName = "Test Parking";
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        // Act
        notification.setParkingId(parkingId);
        notification.setParkingName(parkingName);
        notification.setUserIds(userIds);
        
        // Assert
        assertEquals(parkingId, notification.getParkingId());
        assertEquals(parkingName, notification.getParkingName());
        assertEquals(userIds, notification.getUserIds());
    }
}
