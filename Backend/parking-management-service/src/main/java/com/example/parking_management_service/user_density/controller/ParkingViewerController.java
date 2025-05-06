package com.example.parking_management_service.user_density.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.user_density.dto.ViewerCountDTO;
import com.example.parking_management_service.user_density.service.ParkingViewerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/parking-viewers")
@Tag(name = "parking Viewer Controller", description = "API for tracking users viewing parking details")
public class ParkingViewerController {

    @Autowired
    private ParkingViewerService parkingViewerService;

    @PostMapping("/track")
    @Operation(summary = "Track a user viewing a parking", 
               description = "Registers that a user is viewing a parking detail page and returns the viewer count")
    public ResponseEntity<ViewerCountDTO> trackUserViewing(
            @Parameter(description = "ID of the user viewing the parking") @RequestParam Long userId,
            @Parameter(description = "ID of the parking being viewed") @RequestParam Long parkingId) {
        ViewerCountDTO viewerCount = parkingViewerService.trackUserViewing(userId, parkingId);
        return ResponseEntity.ok(viewerCount);
    }

    @GetMapping("/{parkingId}/count")
    @Operation(summary = "Get viewer count for a parking", 
               description = "Returns the number of users currently viewing a specific parking")
    public ResponseEntity<ViewerCountDTO> getViewerCount(
            @Parameter(description = "ID of the parking to get viewer count for") @PathVariable Long parkingId) {
        ViewerCountDTO viewerCount = parkingViewerService.getViewerCount(parkingId);
        return ResponseEntity.ok(viewerCount);
    }
    
    @GetMapping("/{parkingId}/full")
    @Operation(summary = "Check if a parking is full", 
               description = "Returns whether a specific parking is currently full")
    public ResponseEntity<Boolean> isParkingFull(
            @Parameter(description = "ID of the parking to check") @PathVariable Long parkingId) {
        boolean isFull = parkingViewerService.isParkingFull(parkingId);
        return ResponseEntity.ok(isFull);
    }
}
