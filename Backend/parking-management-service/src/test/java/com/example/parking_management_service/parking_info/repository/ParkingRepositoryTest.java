package com.example.parking_management_service.parking_info.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.parking_management_service.parking_info.model.Parking;

@DataJpaTest
class ParkingRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParkingRepository parkingRepository;

    private Parking testParking;

    @BeforeEach
    void setUp() {
        testParking = new Parking();
        testParking.setName("Test Parking");
        testParking.setLocation("Test Location");
        testParking.setCapacity(100);
        testParking.setOpeningHours("09:00");
        testParking.setClosingHours("18:00");
        testParking.setRate(10.0);
        testParking.setRows(10);
        testParking.setColumns(10);
        entityManager.persist(testParking);
        entityManager.flush();
    }

    @Test
    void whenFindAll_thenReturnAllParkings() {
        List<Parking> parkings = parkingRepository.findAll();
        assertFalse(parkings.isEmpty());
        assertEquals(1, parkings.size());
        assertEquals(testParking.getId(), parkings.get(0).getId());
    }

    @Test
    void whenFindById_thenReturnParking() {
        Optional<Parking> found = parkingRepository.findById(testParking.getId());
        assertTrue(found.isPresent());
        assertEquals(testParking.getId(), found.get().getId());
        assertEquals(testParking.getName(), found.get().getName());
        assertEquals(testParking.getLocation(), found.get().getLocation());
    }
} 