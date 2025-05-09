package com.example.parking_management_service.parking_info.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.parking_info.dto.ParkingLayoutDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotResponseDto;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.service.ParkingSpotService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api")
public class ParkingSpotController {

    @Autowired
    private ParkingSpotService parkingSpotService;
    
    @Operation(
        summary = "Create parking layout",
        description = "Creates a grid layout for a parking lot with specified rows and columns"
    )
    @PostMapping("/admin/parkings/{parkingId}/layout")
    public ResponseEntity<ParkingLayoutDto> createParkingLayout(
            @PathVariable Long parkingId,
            @RequestBody LayoutRequest request) {
        ParkingLayoutDto layout = parkingSpotService.createParkingLayout(parkingId, request.getRows(), request.getColumns());
        return ResponseEntity.ok(layout);
    }
    
    @Operation(
        summary = "Get parking layout",
        description = "Retrieves the layout of a parking lot including all spots and their statuses"
    )
    @GetMapping("/parkings/{parkingId}/layout")
    public ResponseEntity<ParkingLayoutDto> getParkingLayout(@PathVariable Long parkingId) {
        ParkingLayoutDto layout = parkingSpotService.getParkingLayout(parkingId);
        return ResponseEntity.ok(layout);
    }
    
    @Operation(
        summary = "Update spot status",
        description = "Updates the occupied status of a single parking spot"
    )
    @PutMapping("/parkings/{parkingId}/spots/{row}/{column}")
    public ResponseEntity<ParkingSpotDto> updateSpotStatus(
            @PathVariable Long parkingId,
            @PathVariable int row,
            @PathVariable int column,
            @RequestBody SpotStatusRequest request) {
        ParkingSpotDto updatedSpot = parkingSpotService.updateSpotStatus(parkingId, row, column, request.isOccupied());
        return ResponseEntity.ok(updatedSpot);
    }
    
    @Operation(
        summary = "Update multiple spot statuses",
        description = "Updates the occupied status of multiple parking spots at once"
    )
    @PutMapping("/parkings/{parkingId}/spots")
    public ResponseEntity<List<ParkingSpotDto>> updateMultipleSpotStatus(
            @PathVariable Long parkingId,
            @RequestBody List<ParkingSpotDto> spotUpdates) {
        List<ParkingSpotDto> updatedSpots = parkingSpotService.updateMultipleSpotStatus(parkingId, spotUpdates);
        return ResponseEntity.ok(updatedSpots);
    }

    @PostMapping("/{parkingId}/spots")
    public ResponseEntity<ParkingSpotResponseDto> addParkingSpot(
        @PathVariable Long parkingId,
        @RequestBody ParkingSpotDto parkingSpotDto) {
    ParkingSpot parkingSpot = parkingSpotService.addParkingSpotToParking(
        parkingId, 
        parkingSpotDto
    );
    return ResponseEntity.ok(toDto(parkingSpot));
    }

    @PostMapping("/{parkingId}/spots/batch")
    public ResponseEntity<List<ParkingSpotResponseDto>> addParkingSpots(
        @PathVariable Long parkingId,
        @RequestBody List<ParkingSpotDto> parkingSpotDtos) {
        List<ParkingSpot> parkingSpots = parkingSpotService.addParkingSpotsToParking(parkingId, parkingSpotDtos);
        List<ParkingSpotResponseDto> dtos = parkingSpots.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }
    private ParkingSpotResponseDto toDto(ParkingSpot spot) {
        ParkingSpotResponseDto dto = new ParkingSpotResponseDto();
        dto.setId(spot.getId());
        dto.setColumn(spot.getColumn());
        dto.setRow(spot.getRow());
        dto.setOccupied(spot.isOccupied());
        dto.setSpotIdentifier(spot.getSpotIdentifier());
        dto.setSensorId(spot.getSensorId());
        return dto;
    }

    


    // Helper classes for request bodies
    public static class LayoutRequest {
        private int rows;
        private int columns;
        
        public int getRows() {
            return rows;
        }
        
        public void setRows(int rows) {
            this.rows = rows;
        }
        
        public int getColumns() {
            return columns;
        }
        
        public void setColumns(int columns) {
            this.columns = columns;
        }
    }
    
    public static class SpotStatusRequest {
        private boolean occupied;
        
        public boolean isOccupied() {
            return occupied;
        }
        
        public void setOccupied(boolean occupied) {
            this.occupied = occupied;
        }
    }
}
