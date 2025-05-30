package com.example.navigation_service.service.impl;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.service.INavigationService;

import org.springframework.http.HttpMethod;
import com.example.navigation_service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.navigation_service.exception.ErrorResponse;


@Service
public class NavigationServiceImpl implements INavigationService{

    @Autowired
    private RestTemplate restTemplate;

    @Value("${parking.management.service.url:http://parking-management-service:8081}")
    private String parkingManagementServiceUrl;

    // Parking Management servisinden veri çeken yeni metod
    @Override
    public ParkingLocationDto getParkingLocationFromParkingService(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        try {
            String url = parkingManagementServiceUrl + "/api/parkings/location/" + id;
            ParkingLocationDto result = restTemplate.getForObject(url, ParkingLocationDto.class);
            if (result == null) {
                throw new ResourceNotFoundException("Parking location not found with id: " + id);
            }
            return result;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Parking location not found with id: " + id);
        }
    }

     @Override
    public List<ParkingLocationDto> getAllParkingLocationFromParkingService() {
        try {
            String url = parkingManagementServiceUrl + "/api/parkings/location/list";
            List<ParkingLocationDto> locations = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                null, 
                new ParameterizedTypeReference<List<ParkingLocationDto>>() {}
            ).getBody();
            
            if (locations == null || locations.isEmpty()) {
                throw new ResourceNotFoundException("No parking locations found");
            }
            
            return locations;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Parse error response
            try {
                ObjectMapper mapper = new ObjectMapper();
                ErrorResponse errorResponse = mapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                throw new ResourceNotFoundException(errorResponse.getMessage());
            } catch (Exception ex) {
                throw new ResourceNotFoundException("No parking locations found");
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error fetching parking locations");
        }
    }
}
