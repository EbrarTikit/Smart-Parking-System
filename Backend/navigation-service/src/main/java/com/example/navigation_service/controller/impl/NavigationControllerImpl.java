package com.example.navigation_service.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.navigation_service.controller.INavigationController;
import com.example.navigation_service.dto.ParkingLocationDto;
import com.example.navigation_service.service.INavigationService;

@RestController
@RequestMapping("/rest/api/car_park")
public class NavigationControllerImpl implements INavigationController {

    @Autowired
    private INavigationService carParkService;
    
    @GetMapping(path = "/parking-location/{id}")
    @Override
    public ParkingLocationDto getParkingLocationFromParkingService( Long id) {
        return carParkService.getParkingLocationFromParkingService(id);
    }

    @Override
    @GetMapping(path = "/parking-location/list")
    public List<ParkingLocationDto> getAllParkingLocationFromParkingService() {
        return carParkService.getAllParkingLocationFromParkingService();
    }
}
