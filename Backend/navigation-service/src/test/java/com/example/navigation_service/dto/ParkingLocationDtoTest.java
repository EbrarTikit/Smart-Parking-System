package com.example.navigation_service.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkingLocationDtoTest {

    @Test
    void testNoArgsConstructor() {
        // Act
        ParkingLocationDto dto = new ParkingLocationDto();
        
        // Assert
        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getLatitude());
        assertNull(dto.getLongitude());
    }
    
    @Test
    void testAllArgsConstructor() {
        // Arrange
        Long id = 1L;
        String name = "Test Parking";
        Double latitude = 40.0;
        Double longitude = 30.0;
        
        // Act
        ParkingLocationDto dto = new ParkingLocationDto(id, name, latitude, longitude);
        
        // Assert
        assertEquals(id, dto.getId());
        assertEquals(name, dto.getName());
        assertEquals(latitude, dto.getLatitude());
        assertEquals(longitude, dto.getLongitude());
    }
    
    @Test
    void testGettersAndSetters() {
        // Arrange
        ParkingLocationDto dto = new ParkingLocationDto();
        
        // Act
        dto.setId(1L);
        dto.setName("Test Parking");
        dto.setLatitude(40.0);
        dto.setLongitude(30.0);
        
        // Assert
        assertEquals(1L, dto.getId());
        assertEquals("Test Parking", dto.getName());
        assertEquals(40.0, dto.getLatitude());
        assertEquals(30.0, dto.getLongitude());
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Arrange
        ParkingLocationDto dto1 = new ParkingLocationDto(1L, "Test", 40.0, 30.0);
        ParkingLocationDto dto2 = new ParkingLocationDto(1L, "Test", 40.0, 30.0);
        ParkingLocationDto dto3 = new ParkingLocationDto(2L, "Test", 40.0, 30.0);
        
        // Assert
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
    }
}