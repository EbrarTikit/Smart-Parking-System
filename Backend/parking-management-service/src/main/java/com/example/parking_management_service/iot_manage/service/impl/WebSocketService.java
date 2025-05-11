package com.example.parking_management_service.iot_manage.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.parking_info.dto.SensorUpdateDto;


@Service
public class WebSocketService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendParkingSpotUpdate(SensorUpdateDto sensorUpdateDto) {
        messagingTemplate.convertAndSend("/topic/parking-spots", sensorUpdateDto);
    }
}
