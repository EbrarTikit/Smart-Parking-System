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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.parking_management_service.iot_manage.dto.EspDTO;
import com.example.parking_management_service.iot_manage.service.IEspService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EspControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IEspService espService;

    private EspDTO testEspDTO;

    @BeforeEach
    void setUp() {
        testEspDTO = new EspDTO("ESP-001");
        testEspDTO.setEchoPins(Arrays.asList(36, 39, 34, 35));
        testEspDTO.setTriggerPins(Arrays.asList(23, 22, 21, 19));
    }

    @Test
    void addEsp_ShouldReturnCreatedEsp() throws Exception {
        when(espService.addEsp(any(EspDTO.class))).thenReturn(testEspDTO);

        mockMvc.perform(post("/api/iot/esps/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEspDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ESP-001"))
                .andExpect(jsonPath("$.echoPins").isArray())
                .andExpect(jsonPath("$.triggerPins").isArray());
    }

    @Test
    void updateEsp_ShouldReturnUpdatedEsp() throws Exception {
        when(espService.updateEsp(any(EspDTO.class))).thenReturn(testEspDTO);

        mockMvc.perform(put("/api/iot/esps/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEspDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ESP-001"));
    }

    @Test
    void deleteEsp_ShouldReturnNoContent() throws Exception {
        doNothing().when(espService).deleteEsp("ESP-001");

        mockMvc.perform(delete("/api/iot/esps/delete/ESP-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getEsp_ShouldReturnEsp() throws Exception {
        when(espService.getEsp("ESP-001")).thenReturn(testEspDTO);

        mockMvc.perform(get("/api/iot/esps/get/ESP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ESP-001"));
    }

    @Test
    void getAllEsps_ShouldReturnEspList() throws Exception {
        List<EspDTO> esps = Arrays.asList(testEspDTO);
        when(espService.getAllEsps()).thenReturn(esps);

        mockMvc.perform(get("/api/iot/esps/get/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ESP-001"));
    }

    @Test
    void getPinConfiguration_ShouldReturnPinConfig() throws Exception {
        mockMvc.perform(get("/api/iot/esps/pins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echoPins").isArray())
                .andExpect(jsonPath("$.triggerPins").isArray());
    }
} 