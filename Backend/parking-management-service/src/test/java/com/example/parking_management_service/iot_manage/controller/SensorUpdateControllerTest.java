package com.example.parking_management_service.iot_manage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.example.parking_management_service.iot_manage.controller.impl.SensorUpdateController;
import com.example.parking_management_service.iot_manage.dto.SensorUpdateDTO;
import com.example.parking_management_service.iot_manage.service.ISensorUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SensorUpdateController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SensorUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ISensorUpdateService sensorUpdateService;

    private SensorUpdateDTO testSensorUpdateDTO;

    @BeforeEach
    void setUp() {
        testSensorUpdateDTO = new SensorUpdateDTO();
        testSensorUpdateDTO.setParkingId("PARK-001");
        testSensorUpdateDTO.setControllerId("ESP-001");
        testSensorUpdateDTO.setEchoPin(36);
        testSensorUpdateDTO.setTrigPin(23);
        testSensorUpdateDTO.setOccupied(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateParkingSpotOccupancy_ShouldReturnSuccess() throws Exception {
        when(sensorUpdateService.updateParkingSpotOccupancy(any(SensorUpdateDTO.class)))
            .thenReturn(true);

        mockMvc.perform(post("/api/iot/update/spot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSensorUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spot occupancy updated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateParkingSpotOccupancy_ShouldReturnError() throws Exception {
        when(sensorUpdateService.updateParkingSpotOccupancy(any(SensorUpdateDTO.class)))
            .thenReturn(false);

        mockMvc.perform(post("/api/iot/update/spot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSensorUpdateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to update parking spot occupancy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateParkingSpotOccupancyWithRawData_ShouldReturnSuccess() throws Exception {
        when(sensorUpdateService.updateParkingSpotOccupancyFromString(any(String.class)))
            .thenReturn(true);

        mockMvc.perform(post("/api/iot/update/spot/raw")
                .contentType(MediaType.TEXT_PLAIN)
                .content("PARK-001,ESP-001,36,23,true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spot occupancy updated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateParkingSpotOccupancyWithRawData_ShouldReturnError() throws Exception {
        when(sensorUpdateService.updateParkingSpotOccupancyFromString(any(String.class)))
            .thenReturn(false);

        mockMvc.perform(post("/api/iot/update/spot/raw")
                .contentType(MediaType.TEXT_PLAIN)
                .content("PARK-001,ESP-001,36,23,true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to update parking spot occupancy"));
    }
} 