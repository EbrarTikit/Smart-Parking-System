package com.example.parking_management_service.parking_info.dto;

import java.util.List;

import lombok.Data;

@Data
public class LayoutRequestDto {

    private List<ParkingSpotDto> parkingSpots;
    private List<RoadDTO> roads;

}
