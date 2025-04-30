package com.example.parking_management_service.parking_info.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.dto.LocationDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.service.ParkingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api")
public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/parkings/location/{id}")
    public LocationDto getParkingLocation(@PathVariable Long id) {
        return parkingService.getParkingLocation(id);
    }
    
    @GetMapping("/parkings/location/list")
    public List<LocationDto> getAllParkingLocations() {
        return parkingService.getAllParkingLocations();
    }

    //Get All Parkings
    @Operation(summary = "Get all parking lots", description = "Returns a list of all available parking lots")
    @GetMapping("/parkings")
    public ResponseEntity<List<Parking>> getAllParkings() {
        List<Parking> parkings = parkingService.getAllParkings();
        return ResponseEntity.ok(parkings);
    }

    //Get Parking By Id
    @Operation(summary = "Get parking lot by ID", description = "Returns a specific parking lot by its ID")
    @GetMapping("/parkings/{id}")
    public ResponseEntity<Parking> getParkingById(@PathVariable Long id) {
        Parking parking = parkingService.getParkingById(id);
        return ResponseEntity.ok(parking);
    }

    //Create Parking
    @Operation(
        summary = "Create a new parking lot",
        description = "Creates a new parking lot with the provided details. Use string format 'HH:mm' for time values.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" + 
                            "  \"name\": \"Central Parking\",\n" +
                            "  \"location\": \"Downtown\",\n" +
                            "  \"capacity\": 100,\n" +
                            "  \"openingHours\": \"08:00\",\n" +
                            "  \"closingHours\": \"22:00\",\n" +
                            "  \"rate\": 10.50,\n" +
                            "  \"latitude\": 41.0082,\n" +
                            "  \"longitude\": 28.9784\n" +
                            "}"
                )
            )
        )
    )
    @PostMapping("/admin/parkings")
    public ResponseEntity<Parking> createParking(@RequestBody Parking parking) {
        // Ensure ID is null to force auto-generation
        parking.setId(null);
        Parking newParking = parkingService.createParking(parking);
        return ResponseEntity.ok(newParking);
    }

    //Update Parking Infos
    @Operation(summary = "Update parking lot", description = "Updates an existing parking lot with new details")
    @PutMapping("/admin/parkings/{id}")
    public ResponseEntity<Parking> updateParking(@PathVariable Long id, @RequestBody Parking parkingDetails) {
        Parking updatedParking = parkingService.updateParking(id, parkingDetails);
        return ResponseEntity.ok(updatedParking);
    }

    //Delete Parking
    @Operation(summary = "Delete parking lot", description = "Deletes a parking lot by its ID")
    @DeleteMapping("/admin/parkings/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteParking(@PathVariable Long id) {
        parkingService.deleteParking(id);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        
        return ResponseEntity.ok(response);
    }
} 