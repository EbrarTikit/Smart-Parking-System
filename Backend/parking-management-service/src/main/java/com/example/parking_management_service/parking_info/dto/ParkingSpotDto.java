package com.example.parking_management_service.parking_info.dto;

public class ParkingSpotDto {
    private int row;
    private int column;
    private boolean occupied;
    private String spotIdentifier;
    private String sensorId;
    
    public ParkingSpotDto() {
    }
    
    public ParkingSpotDto(int row, int column, boolean occupied, String spotIdentifier) {
        
        this.row = row;
        this.column = column;
        this.occupied = occupied;
        this.spotIdentifier = spotIdentifier;
    }
    
    
    public int getRow() {
        return row;
    }
    
    public void setRow(int row) {
        this.row = row;
    }
    
    public int getColumn() {
        return column;
    }
    
    public void setColumn(int column) {
        this.column = column;
    }
    
    public boolean isOccupied() {
        return occupied;
    }
    
    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
    
    public String getSpotIdentifier() {
        return spotIdentifier;
    }
    
    public void setSpotIdentifier(String spotIdentifier) {
        this.spotIdentifier = spotIdentifier;
    }
    
    public String getSensorId() {
        return sensorId;
    }
    
    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }
}
