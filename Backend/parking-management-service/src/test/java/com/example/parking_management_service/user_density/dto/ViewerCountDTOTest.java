package com.example.parking_management_service.user_density.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewerCountDTOTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange & Act
        ViewerCountDTO dto = new ViewerCountDTO(1L, 5L);
        
        // Assert
        assertEquals(1L, dto.getParkingId());
        assertEquals(5L, dto.getViewerCount());
    }

    @Test
    void testSetters() {
        // Arrange
        ViewerCountDTO dto = new ViewerCountDTO();
        
        // Act
        dto.setParkingId(2L);
        dto.setViewerCount(10L);
        
        // Assert
        assertEquals(2L, dto.getParkingId());
        assertEquals(10L, dto.getViewerCount());
    }
}
