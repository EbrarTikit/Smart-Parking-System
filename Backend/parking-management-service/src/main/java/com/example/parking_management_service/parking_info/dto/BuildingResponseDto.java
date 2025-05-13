package com.example.parking_management_service.parking_info.dto;

import lombok.Data;

@Data
public class BuildingResponseDto {
    private Long id;
    private int buildingColumn;
    private int buildingRow;
}
