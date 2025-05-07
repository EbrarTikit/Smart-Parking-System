package com.example.parking_management_service.iot_manage.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorDTO {
    private String id; // Bu alan sadece okunabilir olacak, otomatik oluşturulacak
    private String parkingId;
    private String controllerId;
    private int echoPin;
    private int trigPin;
}
