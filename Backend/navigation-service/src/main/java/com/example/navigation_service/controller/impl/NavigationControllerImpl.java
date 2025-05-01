package com.example.navigation_service.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<ParkingLocationDto> getParkingLocationFromParkingService(@PathVariable Long id) {
        try {
            ParkingLocationDto location = carParkService.getParkingLocationFromParkingService(id);
            if (location == null || location.getId() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @GetMapping(path = "/parking-location/list")
    public ResponseEntity<List<ParkingLocationDto>> getAllParkingLocationFromParkingService() {
        try {
            List<ParkingLocationDto> locations = carParkService.getAllParkingLocationFromParkingService();
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
