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

import com.example.parking_management_service.parking_info.dto.ParkingLayoutDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingSpotRepository;
import com.example.parking_management_service.iot_manage.service.impl.SensorServiceImpl;

class ParkingSpotServiceTest {
    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private SensorServiceImpl sensorService;

    @InjectMocks
    private ParkingSpotService parkingSpotService;

    private Parking testParking;
    private ParkingSpot testSpot;

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

        testSpot = new ParkingSpot();
        testSpot.setId(1L);
        testSpot.setParking(testParking);
        testSpot.setRow(1);
        testSpot.setColumn(1);
        testSpot.setOccupied(false);
        testSpot.setSpotIdentifier("A1");
    }

    @Test
    void whenCreateParkingLayout_thenReturnLayoutDto() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingSpotRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        ParkingLayoutDto result = parkingSpotService.createParkingLayout(testParking.getId(), 5, 5);
        assertNotNull(result);
        assertEquals(testParking.getId(), result.getParkingId());
        assertEquals(testParking.getName(), result.getParkingName());
        assertEquals(25, result.getCapacity());
        assertEquals(5, result.getRows());
        assertEquals(5, result.getColumns());
    }

    @Test
    void whenGetParkingLayout_thenReturnLayoutDto() {
        List<ParkingSpot> spots = new ArrayList<>();
        spots.add(testSpot);
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingSpotRepository.findByParkingId(testParking.getId())).thenReturn(spots);

        ParkingLayoutDto result = parkingSpotService.getParkingLayout(testParking.getId());
        assertNotNull(result);
        assertEquals(testParking.getId(), result.getParkingId());
        assertEquals(testParking.getName(), result.getParkingName());
        assertEquals(1, result.getSpots().size());
    }

    @Test
    void whenUpdateSpotStatus_thenReturnUpdatedSpotDto() {
        when(parkingSpotRepository.findByParkingIdAndRowAndColumn(testParking.getId(), 1, 1))
            .thenReturn(testSpot);
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenReturn(testSpot);

        ParkingSpotDto result = parkingSpotService.updateSpotStatus(testParking.getId(), 1, 1, true);
        assertNotNull(result);
        assertEquals(1, result.getRow());
        assertEquals(1, result.getColumn());
        assertTrue(result.isOccupied());
    }

    @Test
    void whenUpdateMultipleSpotStatus_thenReturnUpdatedSpotDtos() {
        List<ParkingSpotDto> updates = new ArrayList<>();
        ParkingSpotDto update = new ParkingSpotDto(1, 1, true, "A1");
        updates.add(update);

        when(parkingSpotRepository.findByParkingIdAndRowAndColumn(testParking.getId(), 1, 1))
            .thenReturn(testSpot);
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenReturn(testSpot);

        List<ParkingSpotDto> results = parkingSpotService.updateMultipleSpotStatus(testParking.getId(), updates);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isOccupied());
    }

    @Test
    void whenClearParkingSpotsOfParking_thenSpotsAreCleared() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        parkingSpotService.clearParkingSpotsOfParking(testParking.getId());
        verify(parkingRepository, times(1)).save(any(Parking.class));
    }
} 