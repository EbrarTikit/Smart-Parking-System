package com.example.parking_management_service.parking_info.dto;

import java.util.List;

import lombok.Data;

@Data
public class LayoutResponseDto {
    private Long parkingId;
    private String parkingName;
    private int capacity;
    private int rows;
    private int columns;
    private List<ParkingSpotResponseDto> parkingSpots;
    private List<RoadResponseDto> roads;
}