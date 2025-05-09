package com.example.parking_management_service.parking_info.dto;

import lombok.Data;

@Data
public class RoadResponseDto {
    private Long id;
    private int roadColumn;
    private int roadRow;
}
