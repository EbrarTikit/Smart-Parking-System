package com.example.parking_management_service.parking_info.dto;

import lombok.Data;

@Data
public class LocationDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}
