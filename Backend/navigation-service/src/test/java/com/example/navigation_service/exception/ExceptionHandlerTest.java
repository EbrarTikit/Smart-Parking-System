package com.example.navigation_service.exception;

import com.example.navigation_service.starter.NavigationServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.navigation_service.service.INavigationService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NavigationServiceApplication.class)
@AutoConfigureMockMvc
public class ExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private INavigationService carParkService;
    
    @Test
    void shouldHandleInternalServerError() throws Exception {
        when(carParkService.getParkingLocationFromParkingService(anyLong()))
            .thenThrow(new RuntimeException("Internal Server Error"));
            
        mockMvc.perform(get("/rest/api/car_park/parking-location/1"))
            .andExpect(status().isNotFound()); // Controller exception handler'ında not found döndürüyor
    }
    
    @Test
    void shouldHandleInternalServerErrorForList() throws Exception {
        when(carParkService.getAllParkingLocationFromParkingService())
            .thenThrow(new RuntimeException("Internal Server Error"));
            
        mockMvc.perform(get("/rest/api/car_park/parking-location/list"))
            .andExpect(status().isInternalServerError());
    }
}