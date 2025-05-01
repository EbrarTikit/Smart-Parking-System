package com.example.navigation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLocationDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}
