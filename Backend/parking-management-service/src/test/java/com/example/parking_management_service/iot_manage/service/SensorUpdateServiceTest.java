package com.example.parking_management_service.iot_manage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.parking_management_service.iot_manage.dto.SensorUpdateDTO;
import com.example.parking_management_service.iot_manage.service.impl.SensorUpdateServiceImpl;
import com.example.parking_management_service.iot_manage.service.impl.WebSocketService;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingSpotRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorUpdateServiceTest {

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private SensorUpdateServiceImpl sensorUpdateService;

    private SensorUpdateDTO testSensorUpdateDTO;
    private ParkingSpot testParkingSpot;

    @BeforeEach
    void setUp() {
        testSensorUpdateDTO = new SensorUpdateDTO();
        testSensorUpdateDTO.setParkingId("0001");
        testSensorUpdateDTO.setControllerId("0001");
        testSensorUpdateDTO.setEchoPin(36);
        testSensorUpdateDTO.setTrigPin(23);
        testSensorUpdateDTO.setOccupied(true);

        testParkingSpot = new ParkingSpot();
        testParkingSpot.setId(1L);
        testParkingSpot.setSensorId("000100013623");
        testParkingSpot.setOccupied(false);
    }

    @Test
    void updateParkingSpotOccupancy_ShouldUpdateOccupancy() {
        String sensorId = "000100013623";
        when(parkingSpotRepository.findBySensorId(sensorId))
            .thenReturn(Optional.of(testParkingSpot));
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenReturn(testParkingSpot);

        boolean result = sensorUpdateService.updateParkingSpotOccupancy(testSensorUpdateDTO);

        assertTrue(result);
        verify(parkingSpotRepository).save(any(ParkingSpot.class));
        verify(webSocketService).sendParkingSpotUpdate(any());
    }

    @Test
    void updateParkingSpotOccupancy_ShouldReturnFalse_WhenParkingSpotNotFound() {
        when(parkingSpotRepository.findBySensorId("00010001023623"))
            .thenReturn(Optional.empty());

        boolean result = sensorUpdateService.updateParkingSpotOccupancy(testSensorUpdateDTO);

        assertFalse(result);
        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
        verify(webSocketService, never()).sendParkingSpotUpdate(any());
    }

    @Test
    void updateParkingSpotOccupancyFromString_ShouldUpdateOccupancy() {
        String sensorId = "000100013623";
        when(parkingSpotRepository.findBySensorId(sensorId))
            .thenReturn(Optional.of(testParkingSpot));
        when(parkingSpotRepository.save(any(ParkingSpot.class))).thenReturn(testParkingSpot);

        boolean result = sensorUpdateService.updateParkingSpotOccupancyFromString("0001,0001,36,23,true");

        assertTrue(result);
        verify(parkingSpotRepository).save(any(ParkingSpot.class));
        verify(webSocketService).sendParkingSpotUpdate(any());
    }

    @Test
    void updateParkingSpotOccupancyFromString_ShouldReturnFalse_WhenInvalidFormat() {
        boolean result = sensorUpdateService.updateParkingSpotOccupancyFromString("invalid,format");

        assertFalse(result);
        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
        verify(webSocketService, never()).sendParkingSpotUpdate(any());
    }
} 