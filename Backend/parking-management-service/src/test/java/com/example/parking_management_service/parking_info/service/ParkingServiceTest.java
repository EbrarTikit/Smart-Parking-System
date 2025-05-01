package com.example.parking_management_service.parking_info.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    @Mock
    private ParkingRepository parkingRepository;

    @InjectMocks
    private ParkingService parkingService;

    private Parking testParking;
    private List<Parking> parkingList;

    @BeforeEach
    public void setUp() {
        // Test verilerini hazırla
        testParking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        testParking.setId(1L);

        Parking secondParking = new Parking("Second Parking", "Second Location", 50, "09:00", "21:00", 5.0);
        secondParking.setId(2L);

        parkingList = Arrays.asList(testParking, secondParking);
    }

    @Test
    public void testGetAllParkings() {
        when(parkingRepository.findAll()).thenReturn(parkingList);

        List<Parking> result = parkingService.getAllParkings();

        assertEquals(2, result.size());
        assertEquals("Test Parking", result.get(0).getName());
        assertEquals("Second Parking", result.get(1).getName());
    }

    @Test
    public void testGetParkingById_Success() {
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));

        Parking result = parkingService.getParkingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Parking", result.getName());
    }

    @Test
    public void testGetParkingById_NotFound() {
        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.getParkingById(999L);
        });
        
        assertEquals("Parking not found with id: 999", exception.getMessage());
    }

    @Test
    public void testCreateParking() {
        when(parkingRepository.save(any(Parking.class))).thenReturn(testParking);

        Parking result = parkingService.createParking(new Parking("New Parking", "New Location", 75, "10:00", "18:00", 8.0));

        assertNotNull(result);
        assertEquals("Test Parking", result.getName());
    }

    @Test
    public void testUpdateParking_Success() {
        Parking updatedParking = new Parking("Updated Parking", "Updated Location", 120, "07:00", "22:00", 12.0);
        updatedParking.setId(1L);

        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenReturn(updatedParking);

        Parking result = parkingService.updateParking(1L, updatedParking);

        assertNotNull(result);
        assertEquals("Updated Parking", result.getName());
        assertEquals("Updated Location", result.getLocation());
        assertEquals(120, result.getCapacity());
    }

    @Test
    public void testUpdateParking_NotFound() {
        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.updateParking(999L, new Parking());
        });
        
        assertEquals("Parking not found with id: 999", exception.getMessage());
    }

    @Test
    public void testDeleteParking_Success() {
        when(parkingRepository.findById(1L)).thenReturn(Optional.of(testParking));
        doNothing().when(parkingRepository).delete(any(Parking.class));

        parkingService.deleteParking(1L);

        verify(parkingRepository, times(1)).delete(testParking);
    }

    @Test
    public void testDeleteParking_NotFound() {
        when(parkingRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            parkingService.deleteParking(999L);
        });
        
        assertEquals("Parking not found with id: 999", exception.getMessage());
    }
} 