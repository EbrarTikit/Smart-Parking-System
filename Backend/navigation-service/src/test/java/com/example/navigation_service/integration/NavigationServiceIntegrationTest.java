package com.example.navigation_service.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.service.INavigationService;
import com.example.navigation_service.starter.NavigationServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = NavigationServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class NavigationServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private INavigationService navigationService;

    @Autowired
    private RestTemplate serviceRestTemplate;

    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(serviceRestTemplate);
        objectMapper = new ObjectMapper();
    }

    @Test
    void getParkingLocation_ShouldReturnParkingLocation() throws Exception {
        // Arrange
        Long parkingId = 1L;
        ParkingLocationDto expectedDto = new ParkingLocationDto();
        expectedDto.setId(parkingId);
        expectedDto.setName("Test Parking");
        expectedDto.setLatitude(41.0082);
        expectedDto.setLongitude(28.9784);
        
        String mockResponse = objectMapper.writeValueAsString(expectedDto);
        
        mockServer.expect(MockRestRequestMatchers.requestTo("http://parking-management-service:8081/api/parkings/location/" + parkingId))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess(
                mockResponse,
                MediaType.APPLICATION_JSON
            ));

        String url = "http://localhost:" + port + "/rest/api/car_park/parking-location/" + parkingId;

        // Act
        ResponseEntity<ParkingLocationDto> response = restTemplate.getForEntity(
            url, 
            ParkingLocationDto.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(parkingId, response.getBody().getId());
        assertEquals("Test Parking", response.getBody().getName());
        mockServer.verify();
    }

    @Test
    void getAllParkingLocations_ShouldReturnList() throws Exception {
        // Arrange
        ParkingLocationDto[] expectedDtos = new ParkingLocationDto[] {
            createTestParkingLocation(1L, "Parking 1"),
            createTestParkingLocation(2L, "Parking 2")
        };
        
        String mockResponse = objectMapper.writeValueAsString(expectedDtos);
        
        mockServer.expect(MockRestRequestMatchers.requestTo("http://parking-management-service:8081/api/parkings/location/list"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess(
                mockResponse,
                MediaType.APPLICATION_JSON
            ));

        String url = "http://localhost:" + port + "/rest/api/car_park/parking-location/list";

        // Act
        ResponseEntity<ParkingLocationDto[]> response = restTemplate.getForEntity(
            url, 
            ParkingLocationDto[].class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
        mockServer.verify();
    }

    private ParkingLocationDto createTestParkingLocation(Long id, String name) {
        ParkingLocationDto dto = new ParkingLocationDto();
        dto.setId(id);
        dto.setName(name);
        dto.setLatitude(41.0082);
        dto.setLongitude(28.9784);
        return dto;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}