package com.example.parking_management_service.parking_info.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.parking_management_service.dto.LocationDto;
import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.service.ParkingService;

@SpringBootTest
@Transactional
public class ParkingServiceIntegrationTest {

    @Autowired
    private ParkingService parkingService;
    
    @Autowired
    private ParkingRepository parkingRepository;
    
    private Parking testParking;
    
    @BeforeEach
    void setUp() {
        // Test öncesinde veritabanını temizle
        parkingRepository.deleteAll();
        
        // Test için otopark oluştur
        testParking = new Parking();
        testParking.setName("Test Parking");
        testParking.setLocation("Test Location");
        testParking.setCapacity(100);
        testParking.setOpeningHours("08:00");
        testParking.setClosingHours("20:00");
        testParking.setRate(10.0);
        testParking.setLatitude(41.0082);
        testParking.setLongitude(28.9784);
        
        testParking = parkingRepository.save(testParking);
    }
    
    @Test
    void getAllParkings_ShouldReturnParkings() {
        List<Parking> parkings = parkingService.getAllParkings();
        
        assertEquals(1, parkings.size());
        assertEquals(testParking.getId(), parkings.get(0).getId());
        assertEquals("Test Parking", parkings.get(0).getName());
    }
    
    @Test
    void getParkingById_ShouldReturnParking() {
        Parking parking = parkingService.getParkingById(testParking.getId());
        
        assertEquals(testParking.getId(), parking.getId());
        assertEquals("Test Parking", parking.getName());
        assertEquals("Test Location", parking.getLocation());
        assertEquals(100, parking.getCapacity());
    }
    
    @Test
    void getParkingById_WithInvalidId_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.getParkingById(999L);
        });
    }
    
    @Test
    void getParkingLocation_ShouldReturnLocationDto() {
        LocationDto locationDto = parkingService.getParkingLocation(testParking.getId());
        
        assertEquals(testParking.getId(), locationDto.getId());
        assertEquals("Test Parking", locationDto.getName());
        assertEquals(41.0082, locationDto.getLatitude());
        assertEquals(28.9784, locationDto.getLongitude());
    }
    
    @Test
    void getAllParkingLocations_ShouldReturnLocationDtos() {
        List<LocationDto> locations = parkingService.getAllParkingLocations();
        
        assertEquals(1, locations.size());
        assertEquals(testParking.getId(), locations.get(0).getId());
        assertEquals("Test Parking", locations.get(0).getName());
        assertEquals(41.0082, locations.get(0).getLatitude());
        assertEquals(28.9784, locations.get(0).getLongitude());
    }
    
    @Test
    void createParking_ShouldCreateParking() {
        Parking newParking = new Parking();
        newParking.setName("New Parking");
        newParking.setLocation("New Location");
        newParking.setCapacity(200);
        newParking.setOpeningHours("09:00");
        newParking.setClosingHours("21:00");
        newParking.setRate(15.0);
        newParking.setLatitude(41.1082);
        newParking.setLongitude(28.8784);
        
        Parking createdParking = parkingService.createParking(newParking);
        
        assertNotNull(createdParking.getId());
        assertEquals("New Parking", createdParking.getName());
        assertEquals("New Location", createdParking.getLocation());
        assertEquals(200, createdParking.getCapacity());
        
        // Veritabanında kaydedildiğini doğrula
        Optional<Parking> savedParking = parkingRepository.findById(createdParking.getId());
        assertTrue(savedParking.isPresent());
        assertEquals("New Parking", savedParking.get().getName());
    }
    
    @Test
    void updateParking_ShouldUpdateParking() {
        Parking updateDetails = new Parking();
        updateDetails.setName("Updated Parking");
        updateDetails.setLocation("Updated Location");
        updateDetails.setCapacity(150);
        updateDetails.setOpeningHours("07:00");
        updateDetails.setClosingHours("23:00");
        updateDetails.setRate(20.0);
        
        Parking updatedParking = parkingService.updateParking(testParking.getId(), updateDetails);
        
        assertEquals(testParking.getId(), updatedParking.getId());
        assertEquals("Updated Parking", updatedParking.getName());
        assertEquals("Updated Location", updatedParking.getLocation());
        assertEquals(150, updatedParking.getCapacity());
        assertEquals("07:00", updatedParking.getOpeningHours());
        assertEquals("23:00", updatedParking.getClosingHours());
        assertEquals(20.0, updatedParking.getRate());
        
        // Veritabanında güncellendiğini doğrula
        Optional<Parking> savedParking = parkingRepository.findById(testParking.getId());
        assertTrue(savedParking.isPresent());
        assertEquals("Updated Parking", savedParking.get().getName());
    }
    
    @Test
    void deleteParking_ShouldDeleteParking() {
        parkingService.deleteParking(testParking.getId());
        
        // Veritabanından silindi mi kontrol et
        Optional<Parking> deletedParking = parkingRepository.findById(testParking.getId());
        assertFalse(deletedParking.isPresent());
    }
    
    @Test
    void deleteParking_WithInvalidId_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.deleteParking(999L);
        });
    }
}