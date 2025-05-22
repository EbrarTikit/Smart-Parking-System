package com.example.parking_management_service.parking_info.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;

import com.example.parking_management_service.parking_info.dto.BuildingDto;
import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.service.BuildingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.parking_management_service.parking_info.repository.BuildingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.config.SecurityConfig;

@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class)
class BuildingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private BuildingRepository buildingRepository;

    @MockBean
    private ParkingRepository parkingRepository;

    private Parking testParking;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
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

        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setParking(testParking);
        testBuilding.setBuildingRow(1);
        testBuilding.setBuildingColumn(1);
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getBuildings_ShouldReturnBuildings() throws Exception {
        List<Building> buildings = new ArrayList<>();
        buildings.add(testBuilding);
        when(buildingService.getAllBuildings()).thenReturn(buildings);

        mockMvc.perform(get("/api/buildings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(testBuilding.getId()));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getBuildingById_ShouldReturnBuilding() throws Exception {
        when(buildingService.getBuildingById(testBuilding.getId())).thenReturn(Optional.of(testBuilding));

        mockMvc.perform(get("/api/buildings/{id}", testBuilding.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testBuilding.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBuilding_ShouldReturnCreatedBuilding() throws Exception {
        BuildingDto buildingDto = new BuildingDto(1, 1);
        when(buildingService.saveBuilding(any(Building.class))).thenReturn(testBuilding);

        mockMvc.perform(post("/api/buildings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(buildingDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testBuilding.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBuilding_ShouldReturnNoContent() throws Exception {
        doNothing().when(buildingService).deleteBuilding(testBuilding.getId());

        mockMvc.perform(delete("/api/buildings/{id}", testBuilding.getId()))
            .andExpect(status().isNoContent());
    }
} 