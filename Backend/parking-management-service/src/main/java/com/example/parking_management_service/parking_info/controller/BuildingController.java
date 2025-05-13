package com.example.parking_management_service.parking_info.controller;

import com.example.parking_management_service.parking_info.dto.BuildingDto;
import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @GetMapping
    public List<Building> getAllBuildings() {
        return buildingService.getAllBuildings();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable Long id) {
        Optional<Building> building = buildingService.getBuildingById(id);
        return building.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Building createBuilding(@RequestBody BuildingDto buildingDTO) {
        Building building = new Building();
        building.setBuildingColumn(buildingDTO.getBuildingColumn());
        building.setBuildingRow(buildingDTO.getBuildingRow());
        return buildingService.saveBuilding(building);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parkings/{parkingId}/buildings")
    public ResponseEntity<Building> addBuildingToParking(
            @PathVariable Long parkingId,
            @RequestBody BuildingDto buildingDTO) {
        Building building = buildingService.addBuildingToParking(parkingId, buildingDTO);
        return ResponseEntity.ok(building);
    }

    @PostMapping("/parkings/{parkingId}/buildings/list")
    public ResponseEntity<List<Building>> addBuildingsToParking(
        @PathVariable Long parkingId,
        @RequestBody List<BuildingDto> buildingDTOs) {
        List<Building> buildings = buildingService.addBuildingsToParking(parkingId, buildingDTOs);
        return ResponseEntity.ok(buildings);
    }
}
