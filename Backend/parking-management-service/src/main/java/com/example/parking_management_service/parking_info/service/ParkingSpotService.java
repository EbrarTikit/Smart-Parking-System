package com.example.parking_management_service.parking_info.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.parking_management_service.parking_info.dto.ParkingLayoutDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.parking_info.repository.ParkingSpotRepository;

@Service
public class ParkingSpotService {

    @Autowired
    private ParkingRepository parkingRepository;
    
    @Autowired
    private ParkingSpotRepository parkingSpotRepository;
    
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
                spot.getId(), 
                spot.getRow(), 
                spot.getColumn(), 
                spot.isOccupied(),
                spot.getSpotIdentifier()
            );
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
        
        return new ParkingSpotDto(
            spot.getId(), 
            spot.getRow(), 
            spot.getColumn(), 
            spot.isOccupied(),
            spot.getSpotIdentifier()
        );
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
    
    private String generateSpotIdentifier(int row, int column) {
        // Convert row to letter (0=A, 1=B, etc.) and column to number (starting from 1)
        char rowLetter = (char) ('A' + row);
        int columnNumber = column + 1;
        
        return rowLetter + String.valueOf(columnNumber);
    }
}
