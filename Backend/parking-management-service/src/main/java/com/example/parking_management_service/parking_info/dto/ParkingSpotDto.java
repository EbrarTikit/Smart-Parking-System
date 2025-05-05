package com.example.parking_management_service.parking_info.dto;

public class ParkingSpotDto {
    private Long id;
    private int row;
    private int column;
    private boolean occupied;
    private String spotIdentifier;
    
    public ParkingSpotDto() {
    }
    
    public ParkingSpotDto(Long id, int row, int column, boolean occupied, String spotIdentifier) {
        this.id = id;
        this.row = row;
        this.column = column;
        this.occupied = occupied;
        this.spotIdentifier = spotIdentifier;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
}
