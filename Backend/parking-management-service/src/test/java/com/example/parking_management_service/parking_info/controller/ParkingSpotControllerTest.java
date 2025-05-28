package com.example.parking_management_service.parking_info.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;

import com.example.parking_management_service.parking_info.controller.ParkingSpotController.LayoutRequest;
import com.example.parking_management_service.parking_info.controller.ParkingSpotController.SpotStatusRequest;
import com.example.parking_management_service.parking_info.dto.ParkingLayoutDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.service.ParkingSpotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.parking_management_service.config.SecurityConfig;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(ParkingSpotController.class)
@Import(SecurityConfig.class)
class ParkingSpotControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingSpotService parkingSpotService;

    @MockBean
    private ParkingRepository parkingRepository;

    private Parking testParking;
    private ParkingSpot testSpot;

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

        testSpot = new ParkingSpot();
        testSpot.setId(1L);
        testSpot.setParking(testParking);
        testSpot.setRow(1);
        testSpot.setColumn(1);
        testSpot.setOccupied(false);
        testSpot.setSpotIdentifier("A1");
    }

    @Test
    @WithMockUser(roles = {"USER", "ADMIN"})
    void getParkingLayout_ShouldReturnLayout() throws Exception {
        ParkingLayoutDto layoutDto = new ParkingLayoutDto();
        layoutDto.setParkingId(testParking.getId());
        layoutDto.setParkingName(testParking.getName());
        layoutDto.setCapacity(testParking.getCapacity());
        layoutDto.setRows(testParking.getRows());
        layoutDto.setColumns(testParking.getColumns());
        layoutDto.setSpots(new ArrayList<>());

        when(parkingSpotService.getParkingLayout(testParking.getId())).thenReturn(layoutDto);

        mockMvc.perform(get("/api/parkings/{parkingId}/layout", testParking.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parkingId").value(testParking.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createParkingLayout_ShouldReturnLayout() throws Exception {
        LayoutRequest request = new LayoutRequest();
        request.setRows(5);
        request.setColumns(5);

        ParkingLayoutDto layoutDto = new ParkingLayoutDto();
        layoutDto.setParkingId(testParking.getId());
        layoutDto.setParkingName(testParking.getName());
        layoutDto.setCapacity(25);
        layoutDto.setRows(5);
        layoutDto.setColumns(5);
        layoutDto.setSpots(new ArrayList<>());

        when(parkingSpotService.createParkingLayout(testParking.getId(), request.getRows(), request.getColumns()))
            .thenReturn(layoutDto);

        mockMvc.perform(post("/api/parkings/{parkingId}/layout", testParking.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parkingId").value(testParking.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSpotStatus_ShouldReturnUpdatedSpot() throws Exception {
        SpotStatusRequest request = new SpotStatusRequest();
        request.setOccupied(true);

        ParkingSpotDto spotDto = new ParkingSpotDto(1, 1, true, "A1");
        when(parkingSpotService.updateSpotStatus(testParking.getId(), 1, 1, true))
            .thenReturn(spotDto);

        mockMvc.perform(put("/api/parkings/{parkingId}/spots/{row}/{column}", testParking.getId(), 1, 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.occupied").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMultipleSpotStatus_ShouldReturnUpdatedSpots() throws Exception {
        // Test verilerini hazırla
        List<ParkingSpotDto> updates = new ArrayList<>();
        ParkingSpotDto updateDto = new ParkingSpotDto(1, 1, true, "A1");
        updates.add(updateDto);

        List<ParkingSpotDto> updatedSpots = new ArrayList<>();
        ParkingSpotDto updatedSpot = new ParkingSpotDto(1, 1, true, "A1");
        updatedSpots.add(updatedSpot);

        // Service mock'unu ayarla
        when(parkingSpotService.updateMultipleSpotStatus(eq(testParking.getId()), anyList()))
            .thenReturn(updatedSpots);

        // Test isteğini gönder ve yanıtı kontrol et
        mockMvc.perform(put("/api/parkings/{parkingId}/spots", testParking.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updates)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].row").value(1))
            .andExpect(jsonPath("$[0].column").value(1))
            .andExpect(jsonPath("$[0].occupied").value(true))
            .andExpect(jsonPath("$[0].spotIdentifier").value("A1"));

        // Service metodunun çağrıldığını doğrula
        verify(parkingSpotService, times(1)).updateMultipleSpotStatus(eq(testParking.getId()), anyList());
    }
} 