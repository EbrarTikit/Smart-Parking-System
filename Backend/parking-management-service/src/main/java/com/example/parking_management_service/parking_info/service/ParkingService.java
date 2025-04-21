package com.example.parking_management_service.parking_info.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.parking_info.exception.ResourceNotFoundException;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;

@Service
public class ParkingService {

    @Autowired
    private ParkingRepository parkingRepository;

    //Get All Parkings
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
    public Parking createParking(Parking parking) {
        return parkingRepository.save(parking);
    }

    
    //Update Parking Infos
    public Parking updateParking(Long id, Parking parkingDetails) {
        Parking parking = getParkingById(id);
        
        parking.setName(parkingDetails.getName());
        parking.setLocation(parkingDetails.getLocation());
        parking.setCapacity(parkingDetails.getCapacity());
        parking.setOpeningHours(parkingDetails.getOpeningHours());
        parking.setClosingHours(parkingDetails.getClosingHours());
        parking.setRate(parkingDetails.getRate());
        
        return parkingRepository.save(parking);
    }

    
    //Delete Parking
    public void deleteParking(Long id) {
        Parking parking = getParkingById(id);
        parkingRepository.delete(parking);
    }
} 