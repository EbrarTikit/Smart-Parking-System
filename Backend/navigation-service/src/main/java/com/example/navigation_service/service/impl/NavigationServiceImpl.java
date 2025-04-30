package com.example.navigation_service.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.navigation_service.dto.DtoCarPark;
import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.model.CarPark;
import com.example.navigation_service.repository.INavigationRepository;
import com.example.navigation_service.service.INavigationService;

import org.springframework.http.HttpMethod;


@Service
public class NavigationServiceImpl implements INavigationService{

    @Autowired
    private INavigationRepository carParkRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${parking.management.service.url:http://parking-management-service:8081}")
    private String parkingManagementServiceUrl;

    // Parking Management servisinden veri çeken yeni metod
    @Override
    public ParkingLocationDto getParkingLocationFromParkingService(Long id) {
        String url = parkingManagementServiceUrl + "/api/parkings/location/" + id;
        return restTemplate.getForObject(url, ParkingLocationDto.class);
    }

     @Override
    public List<ParkingLocationDto> getAllParkingLocationFromParkingService() {
        String url = parkingManagementServiceUrl + "/api/parkings/location/list";
        return restTemplate.exchange(
        url, 
        HttpMethod.GET, 
        null, 
        new ParameterizedTypeReference<List<ParkingLocationDto>>() {}
    ).getBody();
    }
}
