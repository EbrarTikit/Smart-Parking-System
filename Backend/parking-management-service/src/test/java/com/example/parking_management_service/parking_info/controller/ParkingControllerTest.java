package com.example.parking_management_service.parking_info.controller;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.parking_management_service.parking_info.config.TestSecurityConfig;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.service.ParkingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ParkingController.class)
@Import(TestSecurityConfig.class)
@WithMockUser
public class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingService parkingService;

    @Autowired
    private ObjectMapper objectMapper;

    private Parking testParking;
    private List<Parking> parkingList;

    @BeforeEach
    public void setUp() {
        // test data
        testParking = new Parking("Test Parking", "Test Location", 100, "08:00", "20:00", 10.0);
        testParking.setId(1L);

        Parking secondParking = new Parking("Second Parking", "Second Location", 50, "09:00", "21:00", 5.0);
        secondParking.setId(2L);

        parkingList = Arrays.asList(testParking, secondParking);
    }

    @Test
    public void testGetAllParkings() throws Exception {
        when(parkingService.getAllParkings()).thenReturn(parkingList);

        mockMvc.perform(get("/api/parkings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Parking"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Second Parking"));
    }

    @Test
    public void testGetParkingById() throws Exception {
        when(parkingService.getParkingById(1L)).thenReturn(testParking);

        mockMvc.perform(get("/api/parkings/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Parking"));
    }

    @Test
    public void testCreateParking() throws Exception {
        when(parkingService.createParking(any(Parking.class))).thenReturn(testParking);

        mockMvc.perform(post("/api/admin/parkings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Parking"));
    }

    @Test
    public void testUpdateParking() throws Exception {
        Parking updatedParking = new Parking("Updated Parking", "Updated Location", 120, "07:00", "22:00", 12.0);
        updatedParking.setId(1L);

        when(parkingService.updateParking(eq(1L), any(Parking.class))).thenReturn(updatedParking);

        mockMvc.perform(put("/api/admin/parkings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedParking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Parking"));
    }

    @Test
    public void testDeleteParking() throws Exception {
        mockMvc.perform(delete("/api/admin/parkings/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }
} 