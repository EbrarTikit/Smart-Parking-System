package com.example.parking_management_service.parking_info.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.RoadRepository;
import com.example.parking_management_service.parking_info.service.RoadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.parking_management_service.config.SecurityConfig;

@WebMvcTest(RoadController.class)
@Import(SecurityConfig.class)
class RoadControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoadService roadService;

    @MockBean
    private RoadRepository roadRepository;

    @MockBean
    private ParkingRepository parkingRepository;

    private Parking testParking;
    private Road testRoad;

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

        testRoad = new Road();
        testRoad.setId(1L);
        testRoad.setParking(testParking);
        testRoad.setRoadRow(1);
        testRoad.setRoadColumn(1);
        testRoad.setRoadIdentifier("R1");
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getRoads_ShouldReturnRoads() throws Exception {
        List<Road> roads = new ArrayList<>();
        roads.add(testRoad);
        when(roadService.getAllRoads()).thenReturn(roads);

        mockMvc.perform(get("/api/roads"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(testRoad.getId()));
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getRoadById_ShouldReturnRoad() throws Exception {
        when(roadService.getRoadById(testRoad.getId())).thenReturn(Optional.of(testRoad));

        mockMvc.perform(get("/api/roads/{id}", testRoad.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testRoad.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoad_ShouldReturnCreatedRoad() throws Exception {
        RoadDTO roadDto = new RoadDTO(1, 1, "R1");
        when(roadService.saveRoad(any(Road.class))).thenReturn(testRoad);

        mockMvc.perform(post("/api/roads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(roadDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testRoad.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRoad_ShouldReturnNoContent() throws Exception {
        doNothing().when(roadService).deleteRoad(testRoad.getId());

        mockMvc.perform(delete("/api/roads/{id}", testRoad.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addRoadToParking_ShouldReturnCreatedRoad() throws Exception {
        RoadDTO roadDto = new RoadDTO(1, 1, "R1");
        when(roadService.addRoadToParking(testParking.getId(), roadDto)).thenReturn(testRoad);

        mockMvc.perform(post("/api/roads/parkings/{parkingId}/roads", testParking.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(roadDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testRoad.getId()));
    }
} 