package com.example.parking_management_service.parking_info.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensorUpdateDto {
    private Long id;
    private boolean occupied;

}
