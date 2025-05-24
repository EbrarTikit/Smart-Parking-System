package com.example.parking_management_service.parking_info.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.BuildingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;

class BuildingServiceTest {
    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private ParkingRepository parkingRepository;

    @InjectMocks
    private BuildingService buildingService;

    private Parking testParking;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testParking = new Parking();
        testParking.setId(1L);
        testParking.setName("Test Parking");
        testParking.setLocation("Test Location");
        testParking.setCapacity(100);
        testParking.setOpeningHours("09:00");
        testParking.setClosingHours("18:00");
        testParking.setRate(10.0);
        testParking.setRows(10);
        testParking.setColumns(10);

        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setBuildingRow(1);
        testBuilding.setBuildingColumn(1);
        testBuilding.setParking(testParking);
    }

    @Test
    void whenGetAllBuildings_thenReturnAllBuildings() {
        List<Building> buildings = new ArrayList<>();
        buildings.add(testBuilding);
        when(buildingRepository.findAll()).thenReturn(buildings);

        List<Building> result = buildingService.getAllBuildings();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testBuilding.getId(), result.get(0).getId());
    }

    @Test
    void whenGetBuildingById_thenReturnBuilding() {
        when(buildingRepository.findById(testBuilding.getId())).thenReturn(Optional.of(testBuilding));

        Optional<Building> result = buildingService.getBuildingById(testBuilding.getId());
        assertTrue(result.isPresent());
        assertEquals(testBuilding.getId(), result.get().getId());
        assertEquals(testBuilding.getBuildingRow(), result.get().getBuildingRow());
        assertEquals(testBuilding.getBuildingColumn(), result.get().getBuildingColumn());
    }

    @Test
    void whenSaveBuilding_thenReturnSavedBuilding() {
        when(buildingRepository.save(any(Building.class))).thenReturn(testBuilding);

        Building result = buildingService.saveBuilding(testBuilding);
        assertNotNull(result);
        assertEquals(testBuilding.getId(), result.getId());
        assertEquals(testBuilding.getBuildingRow(), result.getBuildingRow());
        assertEquals(testBuilding.getBuildingColumn(), result.getBuildingColumn());
    }

    @Test
    void whenDeleteBuilding_thenBuildingIsDeleted() {
        doNothing().when(buildingRepository).deleteById(testBuilding.getId());

        buildingService.deleteBuilding(testBuilding.getId());
        verify(buildingRepository, times(1)).deleteById(testBuilding.getId());
    }
} 