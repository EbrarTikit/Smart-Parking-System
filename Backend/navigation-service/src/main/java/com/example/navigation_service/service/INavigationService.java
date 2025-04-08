package com.example.navigation_service.service;

import java.util.List;

import com.example.navigation_service.dto.DtoCarPark;

public interface INavigationService {
    
    public DtoCarPark getParkLocation(Long id);

    public List<DtoCarPark> getAllParkLocation();
   
}
