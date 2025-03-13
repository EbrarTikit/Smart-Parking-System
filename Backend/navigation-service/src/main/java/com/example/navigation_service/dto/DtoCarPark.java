package com.example.navigation_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoCarPark {
    private String name;
    private String longitude;
    private String latitude;
}
