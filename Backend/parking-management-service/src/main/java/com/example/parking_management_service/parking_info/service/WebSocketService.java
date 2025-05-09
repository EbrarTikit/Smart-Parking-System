package com.example.parking_management_service.parking_info.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import java.util.List;

@Service
public class WebSocketService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendParkingSpotUpdate(ParkingSpotDto spotDto) {
        messagingTemplate.convertAndSend("/topic/parking-spots", spotDto);
    }

    public void sendMultipleParkingSpotUpdates(List<ParkingSpotDto> spotDtos) {
        messagingTemplate.convertAndSend("/topic/parking-spots", spotDtos);
    }
}
