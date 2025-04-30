package com.example.navigation_service.service;


import java.util.List;

import com.example.navigation_service.dto.ParkingLocationDto;

public interface INavigationService {
    
    public ParkingLocationDto getParkingLocationFromParkingService(Long id);
    
    public List<ParkingLocationDto> getAllParkingLocationFromParkingService();
   
}
