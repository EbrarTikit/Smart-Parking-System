package com.example.parking_management_service.parking_info.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.parking_management_service.parking_info.dto.LayoutRequestDto;
import com.example.parking_management_service.parking_info.dto.LocationDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.model.Road;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.util.PositionValidator;
import com.example.parking_management_service.parking_info.dto.BuildingDto;
import com.example.parking_management_service.parking_info.model.Building;

@Service
public class ParkingService {

    @Autowired
    private ParkingRepository parkingRepository;

    @Transactional
    public void clearLayoutOfParking(Long parkingId) {
        clearParkingSpotsOfParking(parkingId);
        clearRoadsOfParking(parkingId);
        clearBuildingsOfParking(parkingId);
    }

    public void createParkingLayout(Long parkingId, LayoutRequestDto layoutRequestDto) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        // Layout var mı kontrolü
        if (!parking.getParkingSpots().isEmpty() || !parking.getRoads().isEmpty() || !parking.getBuildings().isEmpty()) {
            throw new IllegalStateException("This parking already has a layout. Please clear it before creating a new one.");
        }
    
        // Binaları da içerecek şekilde pozisyon doğrulaması yapılır
        List<BuildingDto> buildings = layoutRequestDto.getBuildings() != null ? layoutRequestDto.getBuildings() : new ArrayList<>();
        PositionValidator.validateUniquePositions(layoutRequestDto.getParkingSpots(), layoutRequestDto.getRoads(), buildings);
    
        for (ParkingSpotDto spotDto : layoutRequestDto.getParkingSpots()) {
            PositionValidator.validateWithinBounds(spotDto.getRow(), spotDto.getColumn(), parking.getRows(), parking.getColumns());
            ParkingSpot spot = new ParkingSpot();
            spot.setRow(spotDto.getRow());
            spot.setColumn(spotDto.getColumn());
            spot.setOccupied(spotDto.isOccupied());
            spot.setSpotIdentifier(spotDto.getSpotIdentifier());
            spot.setSensorId(spotDto.getSensorId());
            spot.setParking(parking);
            parking.getParkingSpots().add(spot);
        }
    
        for (RoadDTO roadDto : layoutRequestDto.getRoads()) {
            PositionValidator.validateWithinBounds(roadDto.getRoadRow(), roadDto.getRoadColumn(), parking.getRows(), parking.getColumns());
            Road road = new Road();
            road.setRoadRow(roadDto.getRoadRow());
            road.setRoadColumn(roadDto.getRoadColumn());
            road.setRoadIdentifier(roadDto.getRoadIdentifier());
            road.setParking(parking);
            parking.getRoads().add(road);
        }
    
         for (BuildingDto buildingDto : layoutRequestDto.getBuildings()) {
            PositionValidator.validateWithinBounds(buildingDto.getBuildingRow(), buildingDto.getBuildingColumn(), parking.getRows(), parking.getColumns());
            Building building = new Building();
            building.setBuildingRow(buildingDto.getBuildingRow());
            building.setBuildingColumn(buildingDto.getBuildingColumn());
            building.setParking(parking);
            parking.getBuildings().add(building);
        }
        parkingRepository.save(parking);
    }

    @Transactional
    public void clearParkingSpotsOfParking(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
        parking.getParkingSpots().clear();
        parkingRepository.save(parking);
    }

    
    @Transactional
    public void clearRoadsOfParking(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
        parking.getRoads().clear();
        parkingRepository.save(parking);
    }

    @Transactional
    public void clearBuildingsOfParking(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
        parking.getBuildings().clear();
        parkingRepository.save(parking);
    }

    public void updateParkingLayout(Long parkingId, List<ParkingSpotDto> spotDtos, List<RoadDTO> roadDtos) {
        
        PositionValidator.validateUniquePositions(spotDtos, roadDtos);
    
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        clearParkingSpotsOfParking(parkingId);
        clearRoadsOfParking(parkingId);

        for (ParkingSpotDto spotDto : spotDtos) {
            PositionValidator.validateWithinBounds(spotDto.getRow(), spotDto.getColumn(), parking.getRows(), parking.getColumns());
            ParkingSpot spot = new ParkingSpot();
            spot.setRow(spotDto.getRow());
            spot.setColumn(spotDto.getColumn());
            spot.setOccupied(spotDto.isOccupied());
            spot.setSpotIdentifier(spotDto.getSpotIdentifier());
            spot.setSensorId(spotDto.getSensorId());
            spot.setParking(parking);
            parking.getParkingSpots().add(spot);
        }
    
        
        for (RoadDTO roadDto : roadDtos) {
            PositionValidator.validateWithinBounds(roadDto.getRoadRow(), roadDto.getRoadColumn(), parking.getRows(), parking.getColumns());
            Road road = new Road();
            road.setRoadRow(roadDto.getRoadRow());
            road.setRoadColumn(roadDto.getRoadColumn());
            road.setParking(parking);
            parking.getRoads().add(road);
        }
    
        parkingRepository.save(parking);
    }

    @Autowired
    private ParkingSpotService parkingSpotService;

    public LocationDto getParkingLocation(Long id) {
        Optional<Parking> optionalParking = parkingRepository.findById(id);
        LocationDto locationDto = new LocationDto();
        if (optionalParking.isPresent()) {
            Parking parking = optionalParking.get();
            BeanUtils.copyProperties(parking, locationDto);
            return locationDto;
        }
        throw new ResourceNotFoundException("Parking location not found with id: " + id);
    }

    public List<LocationDto> getAllParkingLocations() {
        List<Parking> parkings = parkingRepository.findAll();
        if (parkings.isEmpty()) {
            throw new ResourceNotFoundException("No parking locations found");
        }
        
        List<LocationDto> locationDtos = new ArrayList<>();
        for (Parking parking : parkings) {
            LocationDto locationDto = new LocationDto();
            BeanUtils.copyProperties(parking, locationDto);
            locationDtos.add(locationDto);
        }
        return locationDtos;
    }

    //Get All Parkings
    @Transactional(readOnly = true)
    public List<Parking> getAllParkings() {
        return parkingRepository.findAll();
    }

    
    //Get Parking By Id
    public Parking getParkingById(Long id) {
        Optional<Parking> optionalParking = parkingRepository.findById(id);
        if (optionalParking.isPresent()) {
            return optionalParking.get();
        } else {
            throw new ResourceNotFoundException("Parking not found with id: " + id);
        }
    }

    
    //Create Parking
    @Transactional
    public Parking createParking(Parking parking) {
        // Extract row and column values before saving
        Integer rows = parking.getRows();
        Integer columns = parking.getColumns();
        
        // Save the parking first to get an ID
        Parking savedParking = parkingRepository.save(parking);
        
        // Check if rows and columns are provided
        if (rows != null && columns != null && rows > 0 && columns > 0) {
            // Create parking layout spots
            parkingSpotService.createParkingLayout(savedParking.getId(), rows, columns);
            
            // Refresh parking object with updated data
            savedParking = parkingRepository.findById(savedParking.getId()).get();
        }
        
        return savedParking;
    }
    
    //Update Parking Infos
    @Transactional
    public Parking updateParking(Long id, Parking parkingDetails) {
        Parking parking = getParkingById(id);
        
        parking.setName(parkingDetails.getName());
        parking.setLocation(parkingDetails.getLocation());
        parking.setOpeningHours(parkingDetails.getOpeningHours());
        parking.setClosingHours(parkingDetails.getClosingHours());
        parking.setRate(parkingDetails.getRate());
        parking.setLatitude(parkingDetails.getLatitude());
        parking.setLongitude(parkingDetails.getLongitude());
        parking.setImageUrl(parkingDetails.getImageUrl());
        
        // Check if rows and columns are being updated
        Integer newRows = parkingDetails.getRows();
        Integer newColumns = parkingDetails.getColumns();
        Integer oldRows = parking.getRows();
        Integer oldColumns = parking.getColumns();
        
        // Save parking first
        Parking updatedParking = parkingRepository.save(parking);
        
        // If rows or columns have changed and are not null, update the layout
        if ((newRows != null && newColumns != null) && 
            (oldRows != newRows || oldColumns != newColumns)) {
            parkingSpotService.createParkingLayout(id, newRows, newColumns);
            // Refresh the parking object after layout update
            updatedParking = parkingRepository.findById(id).get();
        }
        
        return updatedParking;
    }
    
    //Delete Parking
    public void deleteParking(Long id) {
        Parking parking = getParkingById(id);
        parkingRepository.delete(parking);
    }
} 