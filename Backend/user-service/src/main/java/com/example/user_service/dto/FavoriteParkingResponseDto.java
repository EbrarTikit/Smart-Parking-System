package com.example.user_service.dto;

public class FavoriteParkingResponseDto {
    private Long id;
    private String name;
    private String location;
    private String imageUrl;
    private double rate;

    // Default constructor
    public FavoriteParkingResponseDto() {}

    // Constructor with all fields
    public FavoriteParkingResponseDto(Long id, String name, String location, String imageUrl, double rate) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.imageUrl = imageUrl;
        this.rate = rate;
    }

    // Getters and Setters
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}
