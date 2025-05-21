package com.example.parking_management_service.parking_info.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
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

import com.example.parking_management_service.parking_info.dto.LocationDto;
import com.example.parking_management_service.parking_info.dto.LayoutRequestDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.service.ParkingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingService parkingService;

    private Parking testParking;
    private LocationDto testLocationDto;
    private List<LocationDto> testLocationDtos;

    @BeforeEach
    void setUp() {
        // Test parking data
        testParking = new Parking();
        testParking.setId(1L);
        testParking.setName("Test Parking");
        testParking.setRows(10);
        testParking.setColumns(10);
        testParking.setCapacity(100);
        testParking.setRate(10.0);
        testParking.setDescription("Test parking description");

        // Test location data
        testLocationDto = new LocationDto();
        testLocationDto.setId(1L);
        testLocationDto.setName("Test Parking");
        testLocationDto.setLatitude(41.0082);
        testLocationDto.setLongitude(28.9784);

        // Test location list
        testLocationDtos = Arrays.asList(testLocationDto);
    }

    @Test
    void getParkingLocation_ShouldReturnLocationDto() throws Exception {
        when(parkingService.getParkingLocation(1L)).thenReturn(testLocationDto);

        mockMvc.perform(get("/api/parkings/location/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Parking"))
                .andExpect(jsonPath("$.latitude").value(41.0082))
                .andExpect(jsonPath("$.longitude").value(28.9784));
    }

    @Test
    void getAllParkingLocations_ShouldReturnLocationDtoList() throws Exception {
        when(parkingService.getAllParkingLocations()).thenReturn(testLocationDtos);

        mockMvc.perform(get("/api/parkings/location/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Parking"))
                .andExpect(jsonPath("$[0].latitude").value(41.0082))
                .andExpect(jsonPath("$[0].longitude").value(28.9784));
    }

    @Test
    void createParking_ShouldReturnCreatedParking() throws Exception {
        when(parkingService.createParking(any(Parking.class))).thenReturn(testParking);

        mockMvc.perform(post("/api/admin/parkings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Parking"))
                .andExpect(jsonPath("$.capacity").value(100))
                .andExpect(jsonPath("$.rate").value(10.0))
                .andExpect(jsonPath("$.description").value("Test parking description"));
    }

    @Test
    void updateParking_ShouldReturnUpdatedParking() throws Exception {
        when(parkingService.updateParking(any(Long.class), any(Parking.class))).thenReturn(testParking);

        mockMvc.perform(put("/api/admin/parkings/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testParking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Parking"))
                .andExpect(jsonPath("$.capacity").value(100))
                .andExpect(jsonPath("$.rate").value(10.0))
                .andExpect(jsonPath("$.description").value("Test parking description"));
    }

    @Test
    void deleteParking_ShouldReturnNoContent() throws Exception {
        doNothing().when(parkingService).deleteParking(1L);

        mockMvc.perform(delete("/api/admin/parkings/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void createParkingLayout_ShouldReturnLayoutDto() throws Exception {
        LayoutRequestDto layoutRequestDto = new LayoutRequestDto();
        layoutRequestDto.setParkingSpots(new ArrayList<>());
        layoutRequestDto.setRoads(new ArrayList<>());
        layoutRequestDto.setBuildings(new ArrayList<>());
        
        doNothing().when(parkingService).createParkingLayout(any(Long.class), any(LayoutRequestDto.class));
        
        mockMvc.perform(post("/api/{parkingId}/layout", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(layoutRequestDto)))
                .andExpect(status().isOk());
    }
}