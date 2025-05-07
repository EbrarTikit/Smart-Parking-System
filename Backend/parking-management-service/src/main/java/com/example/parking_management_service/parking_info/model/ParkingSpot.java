package com.example.parking_management_service.parking_info.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_spots")
public class ParkingSpot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "parking_id", nullable = false)
    private Parking parking;
    
    @Column(name = "spot_row", nullable = false)
    private int row;
    
    @Column(name = "spot_column", nullable = false)
    private int column;
    
    @Column(name = "is_occupied", nullable = false)
    private boolean isOccupied;
    
    @Column(name = "spot_identifier", nullable = true)
    private String spotIdentifier; // Optional identifier like "A1", "B2", etc.
    
    @Column(name = "sensor_id", nullable = true)
    private String sensorId; // ID of the IoT sensor for this spot
    
    public ParkingSpot() {
    }
    
    public ParkingSpot(Parking parking, int row, int column) {
        this.parking = parking;
        this.row = row;
        this.column = column;
        this.isOccupied = false; // Default to unoccupied
    }
    
    public ParkingSpot(Parking parking, int row, int column, String spotIdentifier) {
        this.parking = parking;
        this.row = row;
        this.column = column;
        this.isOccupied = false;
        this.spotIdentifier = spotIdentifier;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Parking getParking() {
        return parking;
    }

    public void setParking(Parking parking) {
        this.parking = parking;
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
        return isOccupied;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
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
    
    // method to check if spot is available (not occupied)
    public boolean isAvailable() {
        return !isOccupied;
    }
}
