package com.example.parking_management_service.user_density.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_viewers")
public class ParkingViewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parking_id", nullable = false)
    private Long parkingId;

    @Column(name = "viewing_start_time", nullable = false)
    private LocalDateTime viewingStartTime;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "notification_sent", nullable = false)
    private boolean notificationSent;

    public ParkingViewer() {
    }

    public ParkingViewer(Long userId, Long parkingId, LocalDateTime viewingStartTime, LocalDateTime expiryTime) {
        this.userId = userId;
        this.parkingId = parkingId;
        this.viewingStartTime = viewingStartTime;
        this.expiryTime = expiryTime;
        this.notificationSent = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getParkingId() {
        return parkingId;
    }

    public void setParkingId(Long parkingId) {
        this.parkingId = parkingId;
    }

    public LocalDateTime getViewingStartTime() {
        return viewingStartTime;
    }

    public void setViewingStartTime(LocalDateTime viewingStartTime) {
        this.viewingStartTime = viewingStartTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
