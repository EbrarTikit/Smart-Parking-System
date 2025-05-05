package com.example.navigation_service.controller;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.service.INavigationService;
import com.example.navigation_service.starter.NavigationServiceApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Application sınıfını açıkça belirtin
@SpringBootTest(classes = NavigationServiceApplication.class)
@AutoConfigureMockMvc
public class NavigationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private INavigationService carParkService;
    
    
    @Test
    void getParkingLocation_ShouldReturnLocation() throws Exception {
        // Arrange
        ParkingLocationDto locationDto = new ParkingLocationDto(1L, "Test Parking", 40.0, 30.0);
        when(carParkService.getParkingLocationFromParkingService(1L)).thenReturn(locationDto);
        
        // Act & Assert
        mockMvc.perform(get("/rest/api/car_park/parking-location/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Parking"));
    }
    
    @Test
    void getAllParkingLocations_ShouldReturnAllLocations() throws Exception {
        // Arrange
        List<ParkingLocationDto> locations = Arrays.asList(
            new ParkingLocationDto(1L, "Parking A", 40.0, 30.0),
            new ParkingLocationDto(2L, "Parking B", 41.0, 31.0)
        );
        when(carParkService.getAllParkingLocationFromParkingService()).thenReturn(locations);
        
        // Act & Assert
        mockMvc.perform(get("/rest/api/car_park/parking-location/list")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("Parking A"));
    }
    
    @Test
    void getParkingLocation_NotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(carParkService.getParkingLocationFromParkingService(anyLong())).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/rest/api/car_park/parking-location/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getParkingLocation_ExceptionThrown_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(carParkService.getParkingLocationFromParkingService(anyLong()))
            .thenThrow(new RuntimeException("Test exception"));
        
        // Act & Assert
        mockMvc.perform(get("/rest/api/car_park/parking-location/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getAllParkingLocations_ExceptionThrown_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(carParkService.getAllParkingLocationFromParkingService())
            .thenThrow(new RuntimeException("Test exception"));
        
        // Act & Assert
        mockMvc.perform(get("/rest/api/car_park/parking-location/list")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
}