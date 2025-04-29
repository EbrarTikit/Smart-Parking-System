package com.example.navigation_service.controller.impl;


import com.example.navigation_service.dto.ParkingLocationResponseDto;
import com.example.navigation_service.messaging.ParkingLocationRequester;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/navigation")
public class NavigationMessagingController {

    private final ParkingLocationRequester locationRequester;

    public NavigationMessagingController(ParkingLocationRequester locationRequester) {
        this.locationRequester = locationRequester;
    }

    @GetMapping("/parking/{id}")
    public ResponseEntity<ParkingLocationResponseDto> getLocation(@PathVariable Long id) {
        ParkingLocationResponseDto response = locationRequester.requestLocation(id);
        return ResponseEntity.ok(response);
    }
}
