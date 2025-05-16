package com.example.parking_management_service.parking_info.service;

import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.RoadRepository;
import com.example.parking_management_service.parking_info.util.PositionValidator;

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


    public Road addRoadToParking(Long parkingId, RoadDTO roadDto) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        // Validate road position is within the parking bounds
        PositionValidator.validateWithinBounds(roadDto.getRoadRow(), roadDto.getRoadColumn(), 
            parking.getRows(), parking.getColumns());
    
        List<ParkingSpotDto> allSpots = parking.getParkingSpots().stream()
            .map(existingSpot -> {
                ParkingSpotDto dto = new ParkingSpotDto();
                dto.setRow(existingSpot.getRow());
                dto.setColumn(existingSpot.getColumn());
                return dto;
            }).toList();
    
        List<RoadDTO> allRoads = parking.getRoads().stream()
            .map(existingRoad -> {
                RoadDTO dto = new RoadDTO();
                dto.setRoadRow(existingRoad.getRoadRow());
                dto.setRoadColumn(existingRoad.getRoadColumn());
                dto.setRoadIdentifier(existingRoad.getRoadIdentifier());
                return dto;
            }).toList();
    
        
        allRoads = new ArrayList<>(allRoads);
        allRoads.add(roadDto);
    
        PositionValidator.validateUniquePositions(allSpots, allRoads);
    
        Road road = new Road();
        road.setRoadRow(roadDto.getRoadRow());
        road.setRoadColumn(roadDto.getRoadColumn());
        road.setRoadIdentifier(roadDto.getRoadIdentifier());
        road.setParking(parking);
    
        // Önce Road nesnesini kaydedip ID alalım
        Road savedRoad = roadRepository.save(road);
        
        // Kaydedilen Road nesnesini parking'e ekleyelim
        parking.getRoads().add(savedRoad);
        parkingRepository.save(parking);
    
        // ID değeri atanmış Road nesnesini döndürelim
        return savedRoad;
    }

    public List<Road> addRoadsToParking(Long parkingId, List<RoadDTO> roadDTOs) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        List<Road> roads = new ArrayList<>();
    
        for (RoadDTO roadDTO : roadDTOs) {
            // Validate road position is within the parking bounds
            PositionValidator.validateWithinBounds(roadDTO.getRoadRow(), roadDTO.getRoadColumn(), 
                parking.getRows(), parking.getColumns());
            
            Road road = new Road();
            road.setRoadColumn(roadDTO.getRoadColumn());
            road.setRoadRow(roadDTO.getRoadRow());
            road.setRoadIdentifier(roadDTO.getRoadIdentifier());
            road.setParking(parking);
    
            
            parking.getRoads().add(road);
    
            roadRepository.save(road);
            roads.add(road);
        }
    
        parkingRepository.save(parking);
    
        return roads;
    }
}
