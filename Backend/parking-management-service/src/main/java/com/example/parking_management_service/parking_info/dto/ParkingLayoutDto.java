package com.example.parking_management_service.parking_info.dto;

import java.util.List;

public class ParkingLayoutDto {
    private Long parkingId;
    private String parkingName;
    private int capacity;
    private int rows;
    private int columns;
    private List<ParkingSpotDto> spots;
    
    public ParkingLayoutDto() {
    }
    
    public Long getParkingId() {
        return parkingId;
    }
    
    public void setParkingId(Long parkingId) {
        this.parkingId = parkingId;
    }
    
    public String getParkingName() {
        return parkingName;
    }
    
    public void setParkingName(String parkingName) {
        this.parkingName = parkingName;
    }
    
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getRows() {
        return rows;
    }
    
    public void setRows(int rows) {
        this.rows = rows;
    }
    
    public int getColumns() {
        return columns;
    }
    
    public void setColumns(int columns) {
        this.columns = columns;
    }
    
    public List<ParkingSpotDto> getSpots() {
        return spots;
    }
    
    public void setSpots(List<ParkingSpotDto> spots) {
        this.spots = spots;
    }
}
