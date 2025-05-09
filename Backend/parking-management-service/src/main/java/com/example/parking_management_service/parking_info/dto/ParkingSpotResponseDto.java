package com.example.parking_management_service.parking_info.dto;

import lombok.Data;

@Data
public class ParkingSpotResponseDto {
    private Long id;
    private int column;
    private int row;
    private boolean occupied;
    private String spotIdentifier;
    private String sensorId;
}
