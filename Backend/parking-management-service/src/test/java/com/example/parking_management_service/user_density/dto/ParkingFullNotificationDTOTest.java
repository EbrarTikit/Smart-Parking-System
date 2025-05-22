package com.example.parking_management_service.user_density.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParkingFullNotificationDTOTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        Long parkingId = 1L;
        String parkingName = "Test Parking";
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        // Act
        ParkingFullNotificationDTO dto = new ParkingFullNotificationDTO(parkingId, parkingName, userIds);
        
        // Assert
        assertEquals(parkingId, dto.getParkingId());
        assertEquals(parkingName, dto.getParkingName());
        assertEquals(userIds, dto.getUserIds());
    }

    @Test
    void testSetters() {
        // Arrange
        ParkingFullNotificationDTO dto = new ParkingFullNotificationDTO();
        Long parkingId = 1L;
        String parkingName = "Test Parking";
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        
        // Act
        dto.setParkingId(parkingId);
        dto.setParkingName(parkingName);
        dto.setUserIds(userIds);
        
        // Assert
        assertEquals(parkingId, dto.getParkingId());
        assertEquals(parkingName, dto.getParkingName());
        assertEquals(userIds, dto.getUserIds());
    }
}
