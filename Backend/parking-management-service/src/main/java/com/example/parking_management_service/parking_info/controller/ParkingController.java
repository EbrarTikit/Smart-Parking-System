package com.example.parking_management_service.parking_info.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.parking_info.dto.BuildingResponseDto;
import com.example.parking_management_service.parking_info.dto.LayoutRequestDto;
import com.example.parking_management_service.parking_info.dto.LayoutResponseDto;
import com.example.parking_management_service.parking_info.dto.LocationDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotResponseDto;
import com.example.parking_management_service.parking_info.dto.RoadResponseDto;
import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.service.ParkingService;
import com.example.parking_management_service.parking_info.service.ParkingSpotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;


@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3002" })
@RestController
@RequestMapping("/api")
public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private ParkingSpotService parkingSpotService;

    @Autowired
    private ParkingRepository parkingRepository;   

    @Operation(
        summary = "Clear layout of a parking",
        description = "Removes all parking spots and roads from the specified parking lot (empties the layout, does not delete the parking itself)"
    )
    @PutMapping("/{parkingId}/clear-layout")
    public ResponseEntity<Void> clearLayoutOfParking(@PathVariable Long parkingId) {
        parkingService.clearLayoutOfParking(parkingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Clear all roads of a parking",
        description = "Removes all roads from the specified parking lot (empties the road list, does not delete the parking itself)"
    )
    @PutMapping("/parking/{parkingId}/clear-roads")
    public ResponseEntity<Void> clearRoadsOfParking(@PathVariable Long parkingId) {
        parkingService.clearRoadsOfParking(parkingId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
        summary = "Clear all spots of a parking",
        description = "Removes all parking spots from the specified parking lot (empties the spot list, does not delete the parking itself)"
    )
    @PutMapping("/parking/{parkingId}/clear-spots")
    public ResponseEntity<Void> clearParkingSpotsOfParking(@PathVariable Long parkingId) {
        parkingSpotService.clearParkingSpotsOfParking(parkingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Clear all buildings of a parking",
        description = "Removes all buildings from the specified parking lot (empties the building list, does not delete the parking itself)"
    )
    @PutMapping("/parking/{parkingId}/clear-buildings")
    public ResponseEntity<Void> clearBuildingsOfParking(@PathVariable Long parkingId) {
        parkingService.clearBuildingsOfParking(parkingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/parkings/location/{id}")
    public ResponseEntity<LocationDto> getParkingLocation(@PathVariable Long id) {
        LocationDto locationDto = parkingService.getParkingLocation(id);
        return ResponseEntity.ok(locationDto);
    }
    
    @GetMapping("/parkings/location/list")
    public ResponseEntity<List<LocationDto>> getAllParkingLocations() {
        List<LocationDto> locations = parkingService.getAllParkingLocations();
        return ResponseEntity.ok(locations);
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

    @Operation(
    summary = "Create a new parking lot",
    description = "Creates a new parking lot with the provided details. Use string format 'HH:mm' for time values. Optionally include rows and columns to create a layout.",
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
                        "  \"rate\": 10.5,\n" +
                        "  \"latitude\": 41.0082,\n" +
                        "  \"longitude\": 28.9784,\n" +
                        "  \"rows\": 5,\n" +
                        "  \"columns\": 4,\n" +
                        "  \"imageUrl\": \"https://example.com/parking-image.jpg\",\n" +
                        "  \"description\": \"lorem ipsum.\"\n" +
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
    @Operation(
        summary = "Update parking lot", 
        description = "Updates an existing parking lot with new details. Can also update rows and columns to modify layout.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" + 
                            "  \"name\": \"Central Parking Updated\",\n" +
                            "  \"location\": \"Downtown\",\n" +
                            "  \"capacity\": 100,\n" +
                            "  \"openingHours\": \"08:00\",\n" +
                            "  \"closingHours\": \"22:00\",\n" +
                            "  \"rate\": 10.50,\n" +
                            "  \"latitude\": 41.0082,\n" +
                            "  \"longitude\": 28.9784,\n" +
                            "  \"rows\": 6,\n" +
                            "  \"columns\": 5,\n" +
                            "  \"imageUrl\": \"https://example.com/parking-updated-image.jpg\",\n" +
                            "  \"description\": \"lorem ipsum dolor sit amet\"\n" +
                            "}"
                )
            )
        )
    )
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

    @GetMapping("/{parkingId}/layout")
    public ResponseEntity<LayoutResponseDto> getParkingLayout(@PathVariable Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));

        List<ParkingSpotResponseDto> spotDtos = parking.getParkingSpots().stream()
            .map(this::toParkingSpotDto)
            .toList();

        List<RoadResponseDto> roadDtos = parking.getRoads().stream()
            .map(this::toRoadDto)
            .toList();
        
        // Binaları da ekleyelim
        List<BuildingResponseDto> buildingDtos = parking.getBuildings() != null ? 
            parking.getBuildings().stream()
                .map(this::toBuildingDto)
                .toList() : 
            new ArrayList<>();

        LayoutResponseDto response = new LayoutResponseDto();
        response.setParkingId(parking.getId());
        response.setParkingName(parking.getName());
        response.setCapacity(parking.getCapacity());
        response.setRows(parking.getRows());
        response.setColumns(parking.getColumns());
        response.setParkingSpots(spotDtos);
        response.setRoads(roadDtos);
        response.setBuildings(buildingDtos);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{parkingId}/layout")
    public ResponseEntity<Void> createParkingLayout(@PathVariable Long parkingId,@RequestBody LayoutRequestDto layoutRequestDto) {
        parkingService.createParkingLayout(parkingId, layoutRequestDto);
        return ResponseEntity.ok().build();
    }

    
    private ParkingSpotResponseDto toParkingSpotDto(ParkingSpot spot) {
        ParkingSpotResponseDto dto = new ParkingSpotResponseDto();
        dto.setId(spot.getId());
        dto.setColumn(spot.getColumn());
        dto.setRow(spot.getRow());
        dto.setSpotIdentifier(spot.getSpotIdentifier());
        dto.setSensorId(spot.getSensorId());
        dto.setOccupied(spot.isOccupied());
        return dto;
    }

    private RoadResponseDto toRoadDto(Road road) {
        RoadResponseDto dto = new RoadResponseDto();
        dto.setId(road.getId());
        dto.setRoadColumn(road.getRoadColumn());
        dto.setRoadRow(road.getRoadRow());
        dto.setRoadIdentifier(road.getRoadIdentifier());
        return dto;
    }

    private BuildingResponseDto toBuildingDto(Building building) {
        BuildingResponseDto dto = new BuildingResponseDto();
        dto.setId(building.getId());
        dto.setBuildingColumn(building.getBuildingColumn());
        dto.setBuildingRow(building.getBuildingRow());
        return dto;
    }
} 