package com.example.parking_management_service.parking_info.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.model.Parking;

@DataJpaTest
class BuildingRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BuildingRepository buildingRepository;

    private Building testBuilding;
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

        testBuilding = new Building();
        testBuilding.setBuildingRow(1);
        testBuilding.setBuildingColumn(1);
        testBuilding.setParking(testParking);
        entityManager.persist(testBuilding);
        entityManager.flush();
    }

    @Test
    void whenFindAll_thenReturnAllBuildings() {
        List<Building> buildings = buildingRepository.findAll();
        assertFalse(buildings.isEmpty());
        assertEquals(1, buildings.size());
        assertEquals(testBuilding.getId(), buildings.get(0).getId());
    }

    @Test
    void whenFindById_thenReturnBuilding() {
        Building found = buildingRepository.findById(testBuilding.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(testBuilding.getId(), found.getId());
        assertEquals(testBuilding.getBuildingRow(), found.getBuildingRow());
        assertEquals(testBuilding.getBuildingColumn(), found.getBuildingColumn());
    }
} 