package com.example.parking_management_service.parking_info.service;

import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.RoadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoadService {

    @Autowired
    private RoadRepository roadRepository;

    @Autowired
    private ParkingRepository parkingRepository;

    public List<Road> getAllRoads() {
        return roadRepository.findAll();
    }

    public Optional<Road> getRoadById(Long id) {
        return roadRepository.findById(id);
    }

    public Road saveRoad(Road road) {
        return roadRepository.save(road);
    }

    public void deleteRoad(Long id) {
        roadRepository.deleteById(id);
    }

    public Road addRoadToParking(Long parkingId, int roadColumn, int roadRow) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));

        Road road = new Road();
        road.setRoadColumn(roadColumn);
        road.setRoadRow(roadRow);
        road.setParking(parking);

        // İlişkiyi iki taraflı güncellemek için:
        parking.getRoads().add(road);

        roadRepository.save(road);
        parkingRepository.save(parking);

        return road;
    }

    public List<Road> addRoadsToParking(Long parkingId, List<RoadDTO> roadDTOs) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        List<Road> roads = new ArrayList<>();
    
        for (RoadDTO roadDTO : roadDTOs) {
            Road road = new Road();
            road.setRoadColumn(roadDTO.getRoadColumn());
            road.setRoadRow(roadDTO.getRoadRow());
            road.setParking(parking);
    
            // İlişkiyi iki taraflı güncellemek için:
            parking.getRoads().add(road);
    
            roadRepository.save(road);
            roads.add(road);
        }
    
        parkingRepository.save(parking);
    
        return roads;
    }
}
