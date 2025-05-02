package com.example.parking_management_service.parking_info.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.parking_management_service.dto.LocationDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ParkingControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ParkingRepository parkingRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Parking testParking;
    
    @BeforeEach
    void setUp() {
        // MockMvc'yi security ile yapılandır 
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
                
        // Test öncesinde veritabanını temizle
        parkingRepository.deleteAll();
        
        // Test için otopark oluştur
        testParking = new Parking();
        testParking.setName("Test Parking");
        testParking.setLocation("Test Location");
        testParking.setCapacity(100);
        testParking.setOpeningHours("08:00");
        testParking.setClosingHours("20:00");
        testParking.setRate(10.0);
        testParking.setLatitude(41.0082);
        testParking.setLongitude(28.9784);
        
        testParking = parkingRepository.save(testParking);
    }
    
    @Test
    @WithMockUser
    void getAllParkings_ShouldReturnParkings() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/parkings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
                
        // Yanıtı doğrudan kontrol edelim
        String responseContent = result.getResponse().getContentAsString();
        List<Parking> parkings = objectMapper.readValue(responseContent, new TypeReference<List<Parking>>() {});
        
        assertFalse(parkings.isEmpty());
        assertEquals(testParking.getId(), parkings.get(0).getId());
        assertEquals("Test Parking", parkings.get(0).getName());
    }
    
    @Test
    @WithMockUser
    void getParkingById_ShouldReturnParking() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/parkings/{id}", testParking.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
                
        // Yanıtı doğrudan kontrol edelim
        String responseContent = result.getResponse().getContentAsString();
        Parking parking = objectMapper.readValue(responseContent, Parking.class);
        
        assertEquals(testParking.getId(), parking.getId());
        assertEquals("Test Parking", parking.getName());
        assertEquals(100, parking.getCapacity());
    }
    
    @Test
    @WithMockUser
    void getParkingById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/parkings/{id}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void getParkingLocation_ShouldReturnLocationDto() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/parkings/location/{id}", testParking.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
                
        // Yanıtı doğrudan kontrol edelim
        String responseContent = result.getResponse().getContentAsString();
        LocationDto locationDto = objectMapper.readValue(responseContent, LocationDto.class);
        
        assertEquals(testParking.getId(), locationDto.getId());
        assertEquals("Test Parking", locationDto.getName());
        assertEquals(41.0082, locationDto.getLatitude());
        assertEquals(28.9784, locationDto.getLongitude());
    }
    
    @Test
    @WithMockUser
    void getAllParkingLocations_ShouldReturnLocationDtos() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/parkings/location/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Yanıtı doğrudan kontrol edelim
        String responseContent = result.getResponse().getContentAsString();
        List<LocationDto> locations = objectMapper.readValue(responseContent, new TypeReference<List<LocationDto>>() {});
        
        assertFalse(locations.isEmpty());
        assertEquals(testParking.getId(), locations.get(0).getId());
        assertEquals("Test Parking", locations.get(0).getName());
        assertEquals(41.0082, locations.get(0).getLatitude());
        assertEquals(28.9784, locations.get(0).getLongitude());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createParking_ShouldCreateParking() throws Exception {
        Parking newParking = new Parking();
        newParking.setName("New Parking");
        newParking.setLocation("New Location");
        newParking.setCapacity(200);
        newParking.setOpeningHours("09:00");
        newParking.setClosingHours("21:00");
        newParking.setRate(15.0);
        newParking.setLatitude(41.1082);
        newParking.setLongitude(28.8784);
        
        MvcResult result = mockMvc.perform(post("/api/admin/parkings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newParking)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        Parking createdParking = objectMapper.readValue(responseContent, Parking.class);
        
        assertNotNull(createdParking.getId());
        assertEquals("New Parking", createdParking.getName());
        assertEquals("New Location", createdParking.getLocation());
        
        // Veritabanında kaydedildiğini doğrula
        assertTrue(parkingRepository.findById(createdParking.getId()).isPresent());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateParking_ShouldUpdateParking() throws Exception {
        Parking updateDetails = new Parking();
        updateDetails.setName("Updated Parking");
        updateDetails.setLocation("Updated Location");
        updateDetails.setCapacity(150);
        updateDetails.setOpeningHours("07:00");
        updateDetails.setClosingHours("23:00");
        updateDetails.setRate(20.0);
        
        MvcResult result = mockMvc.perform(put("/api/admin/parkings/{id}", testParking.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        Parking updatedParking = objectMapper.readValue(responseContent, Parking.class);
        
        assertEquals(testParking.getId(), updatedParking.getId());
        assertEquals("Updated Parking", updatedParking.getName());
        assertEquals("Updated Location", updatedParking.getLocation());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteParking_ShouldDeleteParking() throws Exception {
        mockMvc.perform(delete("/api/admin/parkings/{id}", testParking.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.deleted").value(true));
        
        // Veritabanından silindi mi kontrol et
        assertFalse(parkingRepository.findById(testParking.getId()).isPresent());
    }
}