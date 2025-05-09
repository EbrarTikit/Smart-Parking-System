package com.example.parking_management_service.parking_info.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadDTO {
    private int roadColumn;
    private int roadRow;

}
