package com.example.parking_management_service.parking_info.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.parking_management_service.parking_info.dto.ParkingLocationResponseDto;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;

@Component
public class ParkingLocationListener {

    private final ParkingRepository parkingRepository;

    public ParkingLocationListener(ParkingRepository parkingRepository) {
        this.parkingRepository = parkingRepository;
    }

    @RabbitListener(queues = "parking.location.queue")
    public ParkingLocationResponseDto handleLocationRequest(Long parkingId) {
        Parking parking = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new RuntimeException("Parking not found"));

                return new ParkingLocationResponseDto(
                    parking.getId(),
                    parking.getLatitude(),
                    parking.getLongitude(),
                    parking.getName()
                );
                
    }
}
