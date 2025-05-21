package com.example.parking_management_service.parking_info.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.parking_management_service.parking_info.dto.LayoutRequestDto;
import com.example.parking_management_service.parking_info.dto.LocationDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.service.ParkingSpotService;

class ParkingServiceTest {
    @Mock
    private ParkingRepository parkingRepository;

    @Mock
    private ParkingSpotService parkingSpotService;

    @InjectMocks
    private ParkingService parkingService;

    private Parking testParking;

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
    }

    @Test
    void whenGetAllParkings_thenReturnAllParkings() {
        List<Parking> parkings = new ArrayList<>();
        parkings.add(testParking);
        when(parkingRepository.findAll()).thenReturn(parkings);

        List<Parking> result = parkingService.getAllParkings();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testParking.getId(), result.get(0).getId());
    }

    @Test
    void whenGetParkingById_thenReturnParking() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));

        Parking result = parkingService.getParkingById(testParking.getId());
        assertNotNull(result);
        assertEquals(testParking.getId(), result.getId());
        assertEquals(testParking.getName(), result.getName());
    }

    @Test
    void whenGetParkingByIdNotFound_thenThrowException() {
        when(parkingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.getParkingById(1L);
        });
    }

    @Test
    void whenCreateParking_thenReturnSavedParking() {
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);
        when(parkingRepository.findById(anyLong())).thenReturn(Optional.of(testParking));

        Parking result = parkingService.createParking(testParking);
        assertNotNull(result);
        assertEquals(testParking.getId(), result.getId());
        assertEquals(testParking.getName(), result.getName());
    }

    @Test
    void whenUpdateParking_thenReturnUpdatedParking() {
        Parking updatedParking = new Parking();
        updatedParking.setId(1L);
        updatedParking.setName("Updated Parking");
        updatedParking.setLocation("Updated Location");
        updatedParking.setCapacity(200);
        updatedParking.setOpeningHours("10:00");
        updatedParking.setClosingHours("19:00");
        updatedParking.setRate(20.0);

        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenReturn(updatedParking);

        Parking result = parkingService.updateParking(testParking.getId(), updatedParking);
        assertNotNull(result);
        assertEquals(updatedParking.getName(), result.getName());
        assertEquals(updatedParking.getLocation(), result.getLocation());
    }

    @Test
    void whenDeleteParking_thenParkingIsDeleted() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        doNothing().when(parkingRepository).delete(testParking);

        parkingService.deleteParking(testParking.getId());
        verify(parkingRepository, times(1)).delete(testParking);
    }

    @Test
    void whenGetParkingLocation_thenReturnLocationDto() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));

        LocationDto result = parkingService.getParkingLocation(testParking.getId());
        assertNotNull(result);
        assertEquals(testParking.getName(), result.getName());
        assertEquals(testParking.getLatitude(), result.getLatitude());
        assertEquals(testParking.getLongitude(), result.getLongitude());
    }

    @Test
    void whenGetAllParkingLocations_thenReturnLocationDtos() {
        List<Parking> parkings = new ArrayList<>();
        parkings.add(testParking);
        when(parkingRepository.findAll()).thenReturn(parkings);

        List<LocationDto> result = parkingService.getAllParkingLocations();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testParking.getName(), result.get(0).getName());
    }

    @Test
    void whenCreateParkingLayout_thenLayoutIsCreated() {
        LayoutRequestDto layoutRequestDto = new LayoutRequestDto();
        layoutRequestDto.setParkingSpots(new ArrayList<>());
        layoutRequestDto.setRoads(new ArrayList<>());
        layoutRequestDto.setBuildings(new ArrayList<>());

        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        parkingService.createParkingLayout(testParking.getId(), layoutRequestDto);
        
        // Tüm save çağrılarını doğrula
        verify(parkingRepository, times(4)).save(any(Parking.class));
    }

    @Test
    void whenClearLayoutOfParking_thenLayoutIsCleared() {
        when(parkingRepository.findById(testParking.getId())).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        parkingService.clearLayoutOfParking(testParking.getId());
        verify(parkingRepository, times(3)).save(any(Parking.class));
    }
} 