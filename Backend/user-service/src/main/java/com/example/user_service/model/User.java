package com.example.user_service.model;


import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;



@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    @Email
    private String email;
    private String password;
    
    // Notification preferences
    @Column(name = "parking_full_notification")
    private boolean parkingFullNotification = false; // Default to false (opt-in)

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FavoriteParking> favoriteParkings = new HashSet<>();

    // Constructor, Getters, Setters
    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isParkingFullNotification() {
        return parkingFullNotification;
    }
    
    public void setParkingFullNotification(boolean parkingFullNotification) {
        this.parkingFullNotification = parkingFullNotification;
    }

    public void addFavoriteParking(FavoriteParking favoriteParking) {
        favoriteParkings.add(favoriteParking);
        favoriteParking.setUser(this);
    }

    public void removeFavoriteParking(FavoriteParking favoriteParking) {
        favoriteParkings.remove(favoriteParking);
        favoriteParking.setUser(null);
    }
}

