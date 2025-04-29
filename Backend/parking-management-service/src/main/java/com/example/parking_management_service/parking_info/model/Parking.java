package com.example.parking_management_service.parking_info.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parkings")
public class Parking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private int capacity;
    
    @Column(name = "opening_hours")
    private String openingHours;  // HH:mm format
    
    @Column(name = "closing_hours")
    private String closingHours;  // HH:mm format
    
    @Column(nullable = false)
    private double rate;
    
    @Column(name = "latitude", nullable = true)
    private Double latitude;  
    
    @Column(name = "longitude", nullable = true)
    private Double longitude;  
    
    public Parking() {
    }
    
    public Parking(String name, String location, int capacity, String openingHours, String closingHours, double rate) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.openingHours = openingHours;
        this.closingHours = closingHours;
        this.rate = rate;
    }
    
   
    public Parking(String name, String location, int capacity, String openingHours, String closingHours, double rate, Double latitude, Double longitude) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.openingHours = openingHours;
        this.closingHours = closingHours;
        this.rate = rate;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getter and Setter    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getClosingHours() {
        return closingHours;
    }

    public void setClosingHours(String closingHours) {
        this.closingHours = closingHours;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
} 