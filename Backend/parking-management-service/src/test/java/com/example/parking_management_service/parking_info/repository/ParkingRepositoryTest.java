package com.example.parking_management_service.parking_info.repository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.parking_management_service.parking_info.model.Parking;

@DataJpaTest
public class ParkingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParkingRepository parkingRepository;

    @Test
    public void testSaveParking() {
        // Given
        Parking parking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        
        // When
        Parking savedParking = parkingRepository.save(parking);
        
        // Then
        assertNotNull(savedParking);
        assertNotNull(savedParking.getId());
        assertEquals("Test Parking", savedParking.getName());
    }

    @Test
    public void testFindById() {
        // Given
        Parking parking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        entityManager.persistAndFlush(parking);
        
        // When
        Optional<Parking> foundParking = parkingRepository.findById(parking.getId());
        
        // Then
        assertTrue(foundParking.isPresent());
        assertEquals("Test Parking", foundParking.get().getName());
    }

    @Test
    public void testFindAll() {
        // Given
        Parking parking1 = new Parking("First Parking", "First Location", 100, "08:00", "20:00", 10.0);
        Parking parking2 = new Parking("Second Parking", "Second Location", 50, "09:00", "21:00", 5.0);
        entityManager.persistAndFlush(parking1);
        entityManager.persistAndFlush(parking2);
        
        // When
        List<Parking> parkings = parkingRepository.findAll();
        
        // Then
        assertEquals(2, parkings.size());
    }

    @Test
    public void testUpdateParking() {
        // Given
        Parking parking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        entityManager.persistAndFlush(parking);
        
        // When
        Parking savedParking = parkingRepository.findById(parking.getId()).get();
        savedParking.setName("Updated Parking");
        savedParking.setCapacity(150);
        parkingRepository.save(savedParking);
        
        // Then
        Parking updatedParking = parkingRepository.findById(parking.getId()).get();
        assertEquals("Updated Parking", updatedParking.getName());
        assertEquals(150, updatedParking.getCapacity());
    }

    @Test
    public void testDeleteParking() {
        // Given
        Parking parking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        entityManager.persistAndFlush(parking);
        
        // When
        parkingRepository.deleteById(parking.getId());
        
        // Then
        Optional<Parking> deletedParking = parkingRepository.findById(parking.getId());
        assertFalse(deletedParking.isPresent());
    }
} 