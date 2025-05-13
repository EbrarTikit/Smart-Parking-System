package com.example.parking_management_service.parking_info.util;

import com.example.parking_management_service.parking_info.dto.BuildingDto;
import com.example.parking_management_service.parking_info.dto.ParkingSpotDto;
import com.example.parking_management_service.parking_info.dto.RoadDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PositionValidator {
    public static void validateUniquePositions(List<ParkingSpotDto> spotDtos, List<RoadDTO> roadDtos) {
        Set<String> uniquePositions = new HashSet<>();
        for (ParkingSpotDto spotDto : spotDtos) {
            String key = spotDto.getRow() + "-" + spotDto.getColumn();
            if (!uniquePositions.add(key)) {
                throw new IllegalArgumentException("More than one spot or road cannot be added at the same position: row=" + spotDto.getRow() + ", column=" + spotDto.getColumn());
            }
        }
        for (RoadDTO roadDto : roadDtos) {
            String key = roadDto.getRoadRow() + "-" + roadDto.getRoadColumn();
            if (!uniquePositions.add(key)) {
                throw new IllegalArgumentException("More than one spot or road cannot be added at the same position: row=" + roadDto.getRoadRow() + ", column=" + roadDto.getRoadColumn());
            }
        }
    }

    public static void validateUniquePositions(List<ParkingSpotDto> spotDtos, List<RoadDTO> roadDtos, List<BuildingDto> buildingDtos) {
        Set<String> uniquePositions = new HashSet<>();
        
        // Önce park yerlerini kontrol et
        for (ParkingSpotDto spotDto : spotDtos) {
            String key = spotDto.getRow() + "-" + spotDto.getColumn();
            if (!uniquePositions.add(key)) {
                throw new IllegalArgumentException("More than one element cannot be added at the same position: row=" + spotDto.getRow() + ", column=" + spotDto.getColumn());
            }
        }
        
        // Sonra yolları kontrol et
        for (RoadDTO roadDto : roadDtos) {
            String key = roadDto.getRoadRow() + "-" + roadDto.getRoadColumn();
            if (!uniquePositions.add(key)) {
                throw new IllegalArgumentException("More than one element cannot be added at the same position: row=" + roadDto.getRoadRow() + ", column=" + roadDto.getRoadColumn());
            }
        }
        
        // Son olarak binaları kontrol et
        for (BuildingDto buildingDto : buildingDtos) {
            String key = buildingDto.getBuildingRow() + "-" + buildingDto.getBuildingColumn();
            if (!uniquePositions.add(key)) {
                throw new IllegalArgumentException("More than one element cannot be added at the same position: row=" + buildingDto.getBuildingRow() + ", column=" + buildingDto.getBuildingColumn());
            }
        }
    }

    public static void validateWithinBounds(int row, int column, int maxRows, int maxColumns) {
        if (row >= maxRows || column >= maxColumns) {
            throw new IllegalArgumentException("Position exceeds parking lot boundaries: row=" + row + ", column=" + column +
                    " (max row=" + (maxRows-1) + ", max column=" + (maxColumns-1) + ")");
        }
    }
}