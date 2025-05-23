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
import com.example.parking_management_service.parking_info.model.ParkingSpot;

@DataJpaTest
class ParkingSpotRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    private Parking testParking;
    private ParkingSpot testSpot;

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

        testSpot = new ParkingSpot();
        testSpot.setRow(1);
        testSpot.setColumn(1);
        testSpot.setOccupied(false);
        testSpot.setSpotIdentifier("A1");
        testSpot.setParking(testParking);
        entityManager.persist(testSpot);
        entityManager.flush();
    }

    @Test
    void whenFindByParkingId_thenReturnParkingSpots() {
        List<ParkingSpot> spots = parkingSpotRepository.findByParkingId(testParking.getId());
        assertFalse(spots.isEmpty());
        assertEquals(1, spots.size());
        assertEquals(testSpot.getId(), spots.get(0).getId());
    }

    @Test
    void whenFindByParkingIdAndRowAndColumn_thenReturnParkingSpot() {
        ParkingSpot found = parkingSpotRepository.findByParkingIdAndRowAndColumn(
            testParking.getId(), testSpot.getRow(), testSpot.getColumn());
        assertNotNull(found);
        assertEquals(testSpot.getId(), found.getId());
        assertEquals(testSpot.getRow(), found.getRow());
        assertEquals(testSpot.getColumn(), found.getColumn());
    }

    @Test
    void whenFindBySensorId_thenReturnParkingSpot() {
        testSpot.setSensorId("SENSOR123");
        entityManager.persist(testSpot);
        entityManager.flush();

        Optional<ParkingSpot> found = parkingSpotRepository.findBySensorId("SENSOR123");
        assertTrue(found.isPresent());
        assertEquals(testSpot.getId(), found.get().getId());
        assertEquals("SENSOR123", found.get().getSensorId());
    }
} 