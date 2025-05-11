package com.example.parking_management_service.parking_info.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.parking_management_service.parking_info.dto.ParkingLayoutDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingSpotRepository;
import com.example.parking_management_service.parking_info.util.PositionValidator;

@Service
public class ParkingSpotService {

    @Autowired
    private ParkingRepository parkingRepository;
    
    @Autowired
    private ParkingSpotRepository parkingSpotRepository;
    

    @Transactional
    public void clearParkingSpotsOfParking(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
        parking.getParkingSpots().clear();
        parkingRepository.save(parking);
    }


    @Transactional
    public ParkingLayoutDto createParkingLayout(Long parkingId, int rows, int columns) {
        Parking parking = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking not found with id: " + parkingId));
        
        // Update parking with row and column info
        parking.setRows(rows);
        parking.setColumns(columns);
        
        // Remove existing spots
        parkingSpotRepository.deleteAll(parkingSpotRepository.findByParkingId(parkingId));
        
        // Save updated parking info
        parkingRepository.save(parking);
        
        // Create and save spots
        List<ParkingSpot> spots = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                String spotId = generateSpotIdentifier(r, c);
                ParkingSpot spot = new ParkingSpot(parking, r, c, spotId);
                spots.add(spot);
            }
        }
        
        // Save all spots at once
        parkingSpotRepository.saveAll(spots);
        
        // Update capacity based on total spots
        parking.setCapacity(rows * columns);
        parkingRepository.save(parking);
        
        // Return layout DTO
        return getParkingLayout(parkingId);
    }
    
    public ParkingLayoutDto getParkingLayout(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking not found with id: " + parkingId));
        
        List<ParkingSpot> spots = parkingSpotRepository.findByParkingId(parkingId);
        
        ParkingLayoutDto layoutDto = new ParkingLayoutDto();
        layoutDto.setParkingId(parking.getId());
        layoutDto.setParkingName(parking.getName());
        layoutDto.setCapacity(parking.getCapacity());
        layoutDto.setRows(parking.getRows() != null ? parking.getRows() : 0);
        layoutDto.setColumns(parking.getColumns() != null ? parking.getColumns() : 0);
        
        List<ParkingSpotDto> spotDtos = new ArrayList<>();
        for (ParkingSpot spot : spots) {
            ParkingSpotDto spotDto = new ParkingSpotDto(
                spot.getRow(), 
                spot.getColumn(), 
                spot.isOccupied(),
                spot.getSpotIdentifier()
            );
            
            spotDto.setSensorId(spot.getSensorId());
            spotDtos.add(spotDto);
        }
        
        layoutDto.setSpots(spotDtos);
        return layoutDto;
    }
    
    @Transactional
    public ParkingSpotDto updateSpotStatus(Long parkingId, int row, int column, boolean isOccupied) {
        ParkingSpot spot = parkingSpotRepository.findByParkingIdAndRowAndColumn(parkingId, row, column);
    
        if (spot == null) {
            throw new ResourceNotFoundException("Parking spot not found at position (" + row + "," + column + ") in parking with id: " + parkingId);
        }
    
        spot.setOccupied(isOccupied);
        parkingSpotRepository.save(spot);
    
        ParkingSpotDto spotDto = new ParkingSpotDto( 
            spot.getRow(), 
            spot.getColumn(), 
            spot.isOccupied(),
            spot.getSpotIdentifier()
        );
        spotDto.setSensorId(spot.getSensorId());
    
    
        return spotDto;
    }
    
    @Transactional
    public List<ParkingSpotDto> updateMultipleSpotStatus(Long parkingId, List<ParkingSpotDto> spotUpdates) {
        List<ParkingSpotDto> updatedSpots = new ArrayList<>();
    
        for (ParkingSpotDto update : spotUpdates) {
            ParkingSpotDto updated = updateSpotStatus(parkingId, update.getRow(), update.getColumn(), update.isOccupied());
            updatedSpots.add(updated);
        }
    
    
        return updatedSpots;
    }
    
    @Transactional
    public ParkingSpotDto assignSensorToSpot(Long parkingId, int row, int column, String sensorId) {
        ParkingSpot spot = parkingSpotRepository.findByParkingIdAndRowAndColumn(parkingId, row, column);
        
        if (spot == null) {
            throw new ResourceNotFoundException("Parking spot not found at position (" + row + "," + column + ") in parking with id: " + parkingId);
        }
        
        spot.setSensorId(sensorId);
        parkingSpotRepository.save(spot);
        
        
        ParkingSpotDto spotDto = new ParkingSpotDto( 
            spot.getRow(), 
            spot.getColumn(), 
            spot.isOccupied(),
            spot.getSpotIdentifier()
        );
        

        spotDto.setSensorId(spot.getSensorId());
        
        return spotDto;
    }
    
    private String generateSpotIdentifier(int row, int column) {
        // Convert row to letter (0=A, 1=B, etc.) and column to number (starting from 1)
        char rowLetter = (char) ('A' + row);
        int columnNumber = column + 1;
        
        return rowLetter + String.valueOf(columnNumber);
    }

    public ParkingSpot addParkingSpotToParking(Long parkingId, ParkingSpotDto spotDto) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));
    
        
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
    
        
        allSpots = new ArrayList<>(allSpots);
        allSpots.add(spotDto);
    
        PositionValidator.validateUniquePositions(allSpots, allRoads);
    
        ParkingSpot spot = new ParkingSpot();
        spot.setRow(spotDto.getRow());
        spot.setColumn(spotDto.getColumn());
        spot.setOccupied(spotDto.isOccupied());
        spot.setSpotIdentifier(spotDto.getSpotIdentifier());
        spot.setSensorId(spotDto.getSensorId());
        spot.setParking(parking);
    
        parking.getParkingSpots().add(spot);
        parkingRepository.save(parking);
    
        return spot;
    }

    public List<ParkingSpot> addParkingSpotsToParking(Long parkingId, List<ParkingSpotDto> parkingSpotDtos) {
        Parking parking = parkingRepository.findById(parkingId)
            .orElseThrow(() -> new RuntimeException("Parking not found"));

        List<ParkingSpot> parkingSpots = new ArrayList<>();

        for (ParkingSpotDto parkingSpotDto : parkingSpotDtos) {
            ParkingSpot parkingSpot = new ParkingSpot();
            parkingSpot.setColumn(parkingSpotDto.getColumn());
            parkingSpot.setRow(parkingSpotDto.getRow());
            parkingSpot.setParking(parking);

            
            parking.getParkingSpots().add(parkingSpot);

            parkingSpotRepository.save(parkingSpot);
            parkingSpots.add(parkingSpot);
        }

        parkingRepository.save(parking);

        return parkingSpots;
    }

    

}
