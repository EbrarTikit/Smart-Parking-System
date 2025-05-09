package com.example.parking_management_service.parking_info.controller;

import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.service.RoadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    @Autowired
    private RoadService roadService;

    @GetMapping
    public List<Road> getAllRoads() {
        return roadService.getAllRoads();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Road> getRoadById(@PathVariable Long id) {
        Optional<Road> road = roadService.getRoadById(id);
        return road.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Road createRoad(@RequestBody RoadDTO roadDTO) {
        Road road = new Road();
        road.setRoadColumn(roadDTO.getRoadColumn());
        road.setRoadRow(roadDTO.getRoadRow());
        return roadService.saveRoad(road);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoad(@PathVariable Long id) {
        roadService.deleteRoad(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parkings/{parkingId}/roads")
    public ResponseEntity<Road> addRoadToParking(
            @PathVariable Long parkingId,
            @RequestBody RoadDTO roadDTO) {
        Road road = roadService.addRoadToParking(parkingId, roadDTO);
        return ResponseEntity.ok(road);
    }

    @PostMapping("/parkings/{parkingId}/roads/list")
    public ResponseEntity<List<Road>> addRoadsToParking(
        @PathVariable Long parkingId,
        @RequestBody List<RoadDTO> roadDTOs) {
        List<Road> roads = roadService.addRoadsToParking(parkingId, roadDTOs);
    return ResponseEntity.ok(roads);
}
}