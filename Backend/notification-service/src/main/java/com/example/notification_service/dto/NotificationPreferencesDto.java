package com.example.notification_service.dto;

public class NotificationPreferencesDto {
    private boolean parkingFullNotification;
    
    public NotificationPreferencesDto() {
    }
    
    public NotificationPreferencesDto(Boolean parkingFullNotification) {
        this.parkingFullNotification = parkingFullNotification;
    }
    
    public Boolean isParkingFullNotification() {
        return parkingFullNotification;
    }
    
    public void setParkingFullNotification(Boolean parkingFullNotification) {
        this.parkingFullNotification = parkingFullNotification;
    }
}