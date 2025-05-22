package com.example.notification_service.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationPreferencesDtoTest {

    @Test
    void testDefaultConstructor() {
        // Act
        NotificationPreferencesDto dto = new NotificationPreferencesDto();
        
        // Assert
        assertFalse(dto.isParkingFullNotification());
    }

    @Test
    void testSetters() {
        // Arrange
        NotificationPreferencesDto dto = new NotificationPreferencesDto();
        
        // Act
        dto.setParkingFullNotification(true);
        
        // Assert
        assertTrue(dto.isParkingFullNotification());
    }
}
