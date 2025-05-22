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
import com.example.parking_management_service.parking_info.model.Road;

@DataJpaTest
class RoadRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoadRepository roadRepository;

    private Parking testParking;
    private Road testRoad;

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

        testRoad = new Road();
        testRoad.setRoadRow(1);
        testRoad.setRoadColumn(1);
        testRoad.setRoadIdentifier("R1");
        testRoad.setParking(testParking);
        entityManager.persist(testRoad);
        entityManager.flush();
    }

    @Test
    void whenFindAll_thenReturnAllRoads() {
        List<Road> roads = roadRepository.findAll();
        assertFalse(roads.isEmpty());
        assertEquals(1, roads.size());
        assertEquals(testRoad.getId(), roads.get(0).getId());
    }

    @Test
    void whenFindById_thenReturnRoad() {
        Optional<Road> found = roadRepository.findById(testRoad.getId());
        assertTrue(found.isPresent());
        assertEquals(testRoad.getId(), found.get().getId());
        assertEquals(testRoad.getRoadRow(), found.get().getRoadRow());
        assertEquals(testRoad.getRoadColumn(), found.get().getRoadColumn());
    }
} 