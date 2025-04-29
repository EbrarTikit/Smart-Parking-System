package com.example.navigation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLocationResponseDto {


    private Long id;
    private double latitude;
    private double longitude;
    private String name;
   

}
