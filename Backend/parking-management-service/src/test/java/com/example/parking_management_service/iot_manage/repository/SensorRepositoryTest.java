package com.example.parking_management_service.iot_manage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.parking_management_service.iot_manage.model.Sensor;

@DataJpaTest
@ActiveProfiles("test")
class SensorRepositoryTest {

    @Autowired
    private ISensorRepository sensorRepository;

    @Test
    void saveSensor_ShouldSaveSensor() {
        // Given
        Sensor sensor = new Sensor();
        sensor.setId("PARK-001-ESP-001-36-23");
        sensor.setParkingId("PARK-001");
        sensor.setControllerId("ESP-001");
        sensor.setEchoPin(36);
        sensor.setTrigPin(23);

        // When
        Sensor savedSensor = sensorRepository.save(sensor);

        // Then
        assertThat(savedSensor).isNotNull();
        assertThat(savedSensor.getId()).isEqualTo("PARK-001-ESP-001-36-23");
    }

    @Test
    void findSensorById_ShouldReturnSensor() {
        // Given
        Sensor sensor = new Sensor();
        sensor.setId("PARK-001-ESP-001-36-23");
        sensor.setParkingId("PARK-001");
        sensor.setControllerId("ESP-001");
        sensor.setEchoPin(36);
        sensor.setTrigPin(23);
        sensorRepository.save(sensor);

        // When
        Optional<Sensor> foundSensor = sensorRepository.findById("PARK-001-ESP-001-36-23");

        // Then
        assertThat(foundSensor).isPresent();
        assertThat(foundSensor.get().getId()).isEqualTo("PARK-001-ESP-001-36-23");
    }
} 