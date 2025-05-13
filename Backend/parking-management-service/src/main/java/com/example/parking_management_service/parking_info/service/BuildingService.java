package com.example.parking_management_service.parking_info.service;

import com.example.parking_management_service.parking_info.dto.BuildingDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.model.Building;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.BuildingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.util.PositionValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BuildingService {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private ParkingRepository parkingRepository;

    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    public Optional<Building> getBuildingById(Long id) {
        return buildingRepository.findById(id);
    }

    public Building saveBuilding(Building building) {
        return buildingRepository.save(building);
    }

    public void deleteBuilding(Long id) {
        buildingRepository.deleteById(id);
    }

    public Building addBuildingToParking(Long parkingId, BuildingDto buildingDto) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        // Pozisyon doğrulaması için mevcut park yerleri ve yolları alalım
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
                return dto;
            }).toList();
    
        // Mevcut binaları alalım
        List<BuildingDto> allBuildings = parking.getBuildings().stream()
            .map(existingBuilding -> {
                BuildingDto dto = new BuildingDto();
                dto.setBuildingRow(existingBuilding.getBuildingRow());
                dto.setBuildingColumn(existingBuilding.getBuildingColumn());
                return dto;
            }).toList();
    
        allBuildings = new ArrayList<>(allBuildings);
        allBuildings.add(buildingDto);
    
        // Pozisyon doğrulaması - Binaların diğer nesnelerle çakışmamasını sağlayalım
        validateUniquePositions(allSpots, allRoads, allBuildings);
    
        Building building = new Building();
        building.setBuildingRow(buildingDto.getBuildingRow());
        building.setBuildingColumn(buildingDto.getBuildingColumn());
        building.setParking(parking);
    
        // Önce Building nesnesini kaydedip ID alalım
        Building savedBuilding = buildingRepository.save(building);
        
        // Kaydedilen Building nesnesini parking'e ekleyelim
        parking.getBuildings().add(savedBuilding);
        parkingRepository.save(parking);
    
        return savedBuilding;
    }

    public List<Building> addBuildingsToParking(Long parkingId, List<BuildingDto> buildingDTOs) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        List<Building> buildings = new ArrayList<>();
    
        for (BuildingDto buildingDTO : buildingDTOs) {
            Building building = new Building();
            building.setBuildingColumn(buildingDTO.getBuildingColumn());
            building.setBuildingRow(buildingDTO.getBuildingRow());
            building.setParking(parking);
    
            // Önce Building nesnesini kaydedip ID alalım
            Building savedBuilding = buildingRepository.save(building);
            
            // Kaydedilen Building nesnesini buildings listesine ekleyelim
            buildings.add(savedBuilding);
            
            // Kaydedilen Building nesnesini parking'e ekleyelim
            parking.getBuildings().add(savedBuilding);
        }
    
        parkingRepository.save(parking);
    
        return buildings;
    }
    
    // Pozisyon doğrulaması için yardımcı metod
    private void validateUniquePositions(List<ParkingSpotDto> spots, List<RoadDTO> roads, List<BuildingDto> buildings) {
        // Tüm pozisyonların benzersiz olduğunu kontrol et
        // Önce mevcut pozisyon doğrulama mekanizmasını kullan
        PositionValidator.validateUniquePositions(spots, roads, buildings);
        
        // Sonra binaların diğer nesnelerle çakışmadığını kontrol et
        for (BuildingDto building : buildings) {
            // Spot ile çakışma kontrolü
            for (ParkingSpotDto spot : spots) {
                if (spot.getRow() == building.getBuildingRow() && spot.getColumn() == building.getBuildingColumn()) {
                    throw new RuntimeException("Building position conflicts with an existing parking spot");
                }
            }
            
            // Road ile çakışma kontrolü
            for (RoadDTO road : roads) {
                if (road.getRoadRow() == building.getBuildingRow() && road.getRoadColumn() == building.getBuildingColumn()) {
                    throw new RuntimeException("Building position conflicts with an existing road");
                }
            }
            
            // Diğer binalarla çakışma kontrolü
            for (BuildingDto otherBuilding : buildings) {
                if (otherBuilding != building && // Kendisiyle karşılaştırmayı önle
                    otherBuilding.getBuildingRow() == building.getBuildingRow() && 
                    otherBuilding.getBuildingColumn() == building.getBuildingColumn()) {
                    throw new RuntimeException("Building position conflicts with another building");
                }
            }
        }
    }
}
