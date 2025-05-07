package com.example.parking_management_service.iot_manage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorUpdateDTO {
    private String parkingId;
    private String controllerId;
    private int echoPin;
    private int trigPin;
    private boolean occupied;
    
    // String formatından nesneye dönüştüren statik metot
    public static SensorUpdateDTO fromString(String data) {
        String[] parts = data.split(",");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid sensor data format. Expected: parkingId,controllerId,echoPin,trigPin,occupied");
        }
        
        SensorUpdateDTO dto = new SensorUpdateDTO();
        dto.setParkingId(parts[0]);
        dto.setControllerId(parts[1]);
        dto.setEchoPin(Integer.parseInt(parts[2]));
        dto.setTrigPin(Integer.parseInt(parts[3]));
        dto.setOccupied(Boolean.parseBoolean(parts[4]));
        
        return dto;
    }
}
