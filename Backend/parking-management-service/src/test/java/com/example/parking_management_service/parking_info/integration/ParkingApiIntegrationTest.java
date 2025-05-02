package com.example.parking_management_service.parking_info.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ParkingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParkingRepository parkingRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Test öncesinde veritabanını temizle
        parkingRepository.deleteAll();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void completeParkingFlow_ShouldWorkCorrectly() throws Exception {
        // 1. Otopark oluştur
        Parking newParking = new Parking();
        newParking.setName("Test Parking");
        newParking.setLocation("Test Location");
        newParking.setCapacity(100);
        newParking.setOpeningHours("08:00");
        newParking.setClosingHours("20:00");
        newParking.setRate(10.0);
        newParking.setLatitude(41.0082);
        newParking.setLongitude(28.9784);
        
        MvcResult createResult = mockMvc.perform(post("/api/admin/parkings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newParking)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        Parking createdParking = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                Parking.class);
        
        Long parkingId = createdParking.getId();
        assertNotNull(parkingId);
        
        // 2. Otoparkı kimliğiyle al
        mockMvc.perform(get("/api/parkings/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(parkingId))
                .andExpect(jsonPath("$.name").value("Test Parking"));
        
        // 3. Otoparkın konum bilgilerini al
        mockMvc.perform(get("/api/parkings/location/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(parkingId))
                .andExpect(jsonPath("$.name").value("Test Parking"))
                .andExpect(jsonPath("$.latitude").value(41.0082))
                .andExpect(jsonPath("$.longitude").value(28.9784));
        
        // 4. Otoparkı güncelle
        Parking updateDetails = new Parking();
        updateDetails.setName("Updated Parking");
        updateDetails.setLocation("Updated Location");
        updateDetails.setCapacity(150);
        updateDetails.setOpeningHours("07:00");
        updateDetails.setClosingHours("23:00");
        updateDetails.setRate(20.0);
        
        mockMvc.perform(put("/api/admin/parkings/{id}", parkingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(parkingId))
                .andExpect(jsonPath("$.name").value("Updated Parking"));
        
        // 5. Tüm otoparkları listele
        mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(parkingId))
                .andExpect(jsonPath("$[0].name").value("Updated Parking"));
        
        // 6. Otoparkı sil
        mockMvc.perform(delete("/api/admin/parkings/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.deleted").value(true));
        
        // 7. Silindikten sonra otoparkı aramaya çalış
        mockMvc.perform(get("/api/parkings/{id}", parkingId))
                .andExpect(status().isNotFound());
    }
}