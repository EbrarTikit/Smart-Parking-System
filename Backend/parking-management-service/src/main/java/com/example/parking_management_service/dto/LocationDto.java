package com.example.parking_management_service.dto;

import lombok.Data;

@Data
public class LocationDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}
