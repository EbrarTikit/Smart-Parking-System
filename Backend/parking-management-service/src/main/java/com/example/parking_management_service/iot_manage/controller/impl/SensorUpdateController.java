package com.example.parking_management_service.iot_manage.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.iot_manage.dto.SensorUpdateDTO;
import com.example.parking_management_service.iot_manage.service.ISensorUpdateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/iot/update")
@Tag(name = "Sensor Update Controller", description = "Endpoints for updating parking spot status based on sensor data")
public class SensorUpdateController {

    @Autowired
    private ISensorUpdateService sensorUpdateService;
    
    @PostMapping("/spot/raw")
    @Operation(
        summary = "Update parking spot occupancy with raw data", 
        description = "Updates the occupancy status of a parking spot using raw string data in the format: " +
                     "parkingId,controllerId,echoPin,trigPin,isOccupied",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(
                    value = "0001,0001,39,22,true"
                )
            )
        )
    )
    public ResponseEntity<String> updateParkingSpotOccupancyWithRawData(@RequestBody String rawData) {
        boolean success = sensorUpdateService.updateParkingSpotOccupancyFromString(rawData.trim());
        
        if (success) {
            return ResponseEntity.ok("Parking spot occupancy updated successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to update parking spot occupancy");
        }
    }
    
    @PostMapping("/spot")
    @Operation(
        summary = "Update parking spot occupancy", 
        description = "Updates the occupancy status of a parking spot based on sensor data. " +
                     "The sensor is identified by parkingId, controllerId, echoPin, and trigPin.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"parkingId\": \"0001\",\n" +
                           "  \"controllerId\": \"0001\",\n" +
                           "  \"echoPin\": 39,\n" +
                           "  \"trigPin\": 22,\n" +
                           "  \"occupied\": true\n" +
                           "}"
                )
            )
        )
    )
    public ResponseEntity<String> updateParkingSpotOccupancy(@RequestBody SensorUpdateDTO sensorData) {
        boolean success = sensorUpdateService.updateParkingSpotOccupancy(sensorData);
        
        if (success) {
            return ResponseEntity.ok("Parking spot occupancy updated successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to update parking spot occupancy");
        }
    }
}
