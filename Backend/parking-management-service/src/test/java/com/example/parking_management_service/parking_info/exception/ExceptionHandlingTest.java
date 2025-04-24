package com.example.parking_management_service.parking_info.exception;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.parking_management_service.parking_info.config.TestSecurityConfig;
import com.example.parking_management_service.parking_info.controller.ParkingController;
import com.example.parking_management_service.parking_info.service.ParkingService;

@WebMvcTest(ParkingController.class)
@Import(TestSecurityConfig.class)
@WithMockUser
public class ExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingService parkingService;

    //Test for ResourceNotFoundException
    @Test
    public void testResourceNotFoundException() throws Exception {
        when(parkingService.getParkingById(999L))
                .thenThrow(new ResourceNotFoundException("Parking not found with id: 999"));

        
        MvcResult result = mockMvc.perform(get("/api/parkings/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("Parking not found with id: 999"));
    }


    //Test for BadRequestException
    @Test
    public void testBadRequestException() throws Exception {
        
        when(parkingService.getParkingById(0L))
                .thenThrow(new BadRequestException("Invalid parking ID: 0"));


        mockMvc.perform(get("/api/parkings/0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid parking ID: 0"));
    }

    //Test for GeneralException
    @Test
    public void testGeneralException() throws Exception {
        
        when(parkingService.getParkingById(1L))
                .thenThrow(new RuntimeException("Unexpected error"));

        
        mockMvc.perform(get("/api/parkings/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
} 