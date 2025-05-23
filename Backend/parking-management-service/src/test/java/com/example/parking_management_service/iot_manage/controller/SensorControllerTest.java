package com.example.parking_management_service.iot_manage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.parking_management_service.config.TestSecurityConfig;
import com.example.parking_management_service.iot_manage.controller.impl.SensorControllerImpl;
import com.example.parking_management_service.iot_manage.dto.SensorDTO;
import com.example.parking_management_service.iot_manage.service.ISensorService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SensorControllerImpl.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ISensorService sensorService;

    private SensorDTO testSensorDTO;

    @BeforeEach
    void setUp() {
        testSensorDTO = new SensorDTO();
        testSensorDTO.setId("PARK-001-ESP-001-36-23");
        testSensorDTO.setParkingId("PARK-001");
        testSensorDTO.setControllerId("ESP-001");
        testSensorDTO.setEchoPin(36);
        testSensorDTO.setTrigPin(23);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addSensor_ShouldReturnCreatedSensor() throws Exception {
        when(sensorService.addSensor(any(SensorDTO.class)))
            .thenReturn(testSensorDTO);

        mockMvc.perform(post("/api/iot/sensors/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSensorDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("PARK-001-ESP-001-36-23"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSensor_ShouldReturnUpdatedSensor() throws Exception {
        when(sensorService.updateSensor(any(SensorDTO.class)))
            .thenReturn(testSensorDTO);

        mockMvc.perform(put("/api/iot/sensors/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSensorDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PARK-001-ESP-001-36-23"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSensor_ShouldReturnNoContent() throws Exception {
        doNothing().when(sensorService).deleteSensor("PARK-001-ESP-001-36-23");

        mockMvc.perform(delete("/api/iot/sensors/delete/PARK-001-ESP-001-36-23"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void getSensor_ShouldReturnSensor() throws Exception {
        when(sensorService.getSensor("PARK-001-ESP-001-36-23"))
            .thenReturn(testSensorDTO);

        mockMvc.perform(get("/api/iot/sensors/get/PARK-001-ESP-001-36-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PARK-001-ESP-001-36-23"));
    }

    @Test
    @WithMockUser
    void getAllSensors_ShouldReturnSensorList() throws Exception {
        List<SensorDTO> sensors = Arrays.asList(testSensorDTO);
        when(sensorService.getAllSensors()).thenReturn(sensors);

        mockMvc.perform(get("/api/iot/sensors/get/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("PARK-001-ESP-001-36-23"));
    }
} 