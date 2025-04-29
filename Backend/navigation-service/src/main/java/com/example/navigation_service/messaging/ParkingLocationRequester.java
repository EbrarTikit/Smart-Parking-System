package com.example.navigation_service.messaging;

import com.example.navigation_service.dto.ParkingLocationResponseDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ParkingLocationRequester {
    
    private final RabbitTemplate rabbitTemplate;

    
    public ParkingLocationRequester(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public ParkingLocationResponseDto requestLocation(Long parkingId) {
        return (ParkingLocationResponseDto) rabbitTemplate.convertSendAndReceive(
                "parking.exchange",
                "parking.location.request",
                parkingId
        );
    }
}
