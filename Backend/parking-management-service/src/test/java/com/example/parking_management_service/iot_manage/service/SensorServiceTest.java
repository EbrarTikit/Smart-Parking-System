package com.example.parking_management_service.iot_manage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.parking_management_service.iot_manage.dto.SensorDTO;
import com.example.parking_management_service.iot_manage.model.Sensor;
import com.example.parking_management_service.iot_manage.repository.ISensorRepository;
import com.example.parking_management_service.iot_manage.service.impl.SensorServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorServiceTest {

    @Mock
    private ISensorRepository sensorRepository;

    @InjectMocks
    private SensorServiceImpl sensorService;

    private SensorDTO testSensorDTO;
    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        testSensorDTO = new SensorDTO();
        testSensorDTO.setId("000100013623");
        testSensorDTO.setParkingId("0001");
        testSensorDTO.setControllerId("0001");
        testSensorDTO.setEchoPin(36);
        testSensorDTO.setTrigPin(23);

        testSensor = new Sensor();
        testSensor.setId("000100013623");
        testSensor.setParkingId("0001");
        testSensor.setControllerId("0001");
        testSensor.setEchoPin(36);
        testSensor.setTrigPin(23);
    }

    @Test
    void addSensor_ShouldSaveSensor() {
        when(sensorRepository.save(any(Sensor.class))).thenReturn(testSensor);

        SensorDTO result = sensorService.addSensor(testSensorDTO);

        assertNotNull(result);
        assertEquals("000100013623", result.getId());
        verify(sensorRepository).save(any(Sensor.class));
    }

    @Test
    void updateSensor_ShouldUpdateSensor() {
        String sensorId = "000100013623";
        when(sensorRepository.existsById(sensorId)).thenReturn(true);
        when(sensorRepository.save(any(Sensor.class))).thenReturn(testSensor);

        SensorDTO result = sensorService.updateSensor(testSensorDTO);

        assertNotNull(result);
        assertEquals(sensorId, result.getId());
        verify(sensorRepository).save(any(Sensor.class));
    }

    @Test
    void updateSensor_ShouldThrowException_WhenSensorNotFound() {
        String sensorId = "000100013623";
        when(sensorRepository.existsById(sensorId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> sensorService.updateSensor(testSensorDTO));
    }

    @Test
    void deleteSensor_ShouldDeleteSensor() {
        doNothing().when(sensorRepository).deleteById("000100013623");

        sensorService.deleteSensor("000100013623");

        verify(sensorRepository).deleteById("000100013623");
    }

    @Test
    void getSensor_ShouldReturnSensor() {
        String sensorId = "000100013623";
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.of(testSensor));

        SensorDTO result = sensorService.getSensor(sensorId);

        assertNotNull(result);
        assertEquals(sensorId, result.getId());
    }

    @Test
    void getSensor_ShouldThrowException_WhenSensorNotFound() {
        String sensorId = "000100013623";
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sensorService.getSensor(sensorId));
    }

    @Test
    void getAllSensors_ShouldReturnSensorList() {
        List<Sensor> sensors = Arrays.asList(testSensor);
        when(sensorRepository.findAll()).thenReturn(sensors);

        List<SensorDTO> result = sensorService.getAllSensors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("000100013623", result.get(0).getId());
    }
} 