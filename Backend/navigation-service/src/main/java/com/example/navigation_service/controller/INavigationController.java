package com.example.navigation_service.controller;

import java.util.List;

import com.example.navigation_service.dto.ParkingLocationDto;
import org.springframework.http.ResponseEntity;

public interface INavigationController {

    public ResponseEntity<ParkingLocationDto> getParkingLocationFromParkingService(Long id);

    public ResponseEntity<List<ParkingLocationDto>> getAllParkingLocationFromParkingService();

}
