package com.example.navigation_service.controller;

import java.util.List;

import com.example.navigation_service.dto.ParkingLocationDto;

public interface INavigationController {

    public ParkingLocationDto getParkingLocationFromParkingService(Long id);

    public List<ParkingLocationDto> getAllParkingLocationFromParkingService();

}
