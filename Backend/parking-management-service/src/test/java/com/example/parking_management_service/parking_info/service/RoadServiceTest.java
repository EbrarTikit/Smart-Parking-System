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

import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.RoadRepository;

class RoadServiceTest {
    @Mock
    private RoadRepository roadRepository;

    @Mock
    private ParkingRepository parkingRepository;

    @InjectMocks
    private RoadService roadService;

    private Parking testParking;
    private Road testRoad;

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

        testRoad = new Road();
        testRoad.setId(1L);
        testRoad.setParking(testParking);
        testRoad.setRoadRow(1);
        testRoad.setRoadColumn(1);
        testRoad.setRoadIdentifier("R1");
    }

    @Test
    void whenGetAllRoads_thenReturnAllRoads() {
        List<Road> roads = new ArrayList<>();
        roads.add(testRoad);
        when(roadRepository.findAll()).thenReturn(roads);

        List<Road> result = roadService.getAllRoads();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testRoad.getId(), result.get(0).getId());
    }

    @Test
    void whenGetRoadById_thenReturnRoad() {
        when(roadRepository.findById(testRoad.getId())).thenReturn(Optional.of(testRoad));

        Optional<Road> result = roadService.getRoadById(testRoad.getId());
        assertTrue(result.isPresent());
        assertEquals(testRoad.getId(), result.get().getId());
    }

    @Test
    void whenSaveRoad_thenReturnSavedRoad() {
        when(roadRepository.save(any(Road.class))).thenReturn(testRoad);

        Road result = roadService.saveRoad(testRoad);
        assertNotNull(result);
        assertEquals(testRoad.getId(), result.getId());
    }

    @Test
    void whenDeleteRoad_thenRoadIsDeleted() {
        doNothing().when(roadRepository).deleteById(testRoad.getId());

        roadService.deleteRoad(testRoad.getId());
        verify(roadRepository, times(1)).deleteById(testRoad.getId());
    }

    @Test
    void whenAddRoadToParking_thenRoadIsAdded() {
        RoadDTO roadDto = new RoadDTO(1, 1, "R1");
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(roadRepository.save(any(Road.class))).thenReturn(testRoad);
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        Road result = roadService.addRoadToParking(testParking.getId(), roadDto);
        assertNotNull(result);
        assertEquals(testRoad.getId(), result.getId());
        assertEquals(testRoad.getRoadRow(), result.getRoadRow());
        assertEquals(testRoad.getRoadColumn(), result.getRoadColumn());
    }

    @Test
    void whenAddRoadsToParking_thenRoadsAreAdded() {
        List<RoadDTO> roadDtos = new ArrayList<>();
        roadDtos.add(new RoadDTO(1, 1, "R1"));
        roadDtos.add(new RoadDTO(2, 2, "R2"));

        List<Road> roads = new ArrayList<>();
        roads.add(testRoad);
        roads.add(new Road());

        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(roadRepository.save(any(Road.class))).thenReturn(testRoad);
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        List<Road> result = roadService.addRoadsToParking(testParking.getId(), roadDtos);
        assertNotNull(result);
        assertEquals(2, result.size());
    }
} 