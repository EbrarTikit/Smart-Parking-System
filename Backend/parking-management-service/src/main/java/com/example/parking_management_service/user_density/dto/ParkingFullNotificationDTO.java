package com.example.parking_management_service.user_density.dto;

import java.util.List;

public class ParkingFullNotificationDTO {
    private Long parkingId;
    private String parkingName;
    private List<Long> userIds;

    public ParkingFullNotificationDTO() {
    }

    public ParkingFullNotificationDTO(Long parkingId, String parkingName, List<Long> userIds) {
        this.parkingId = parkingId;
        this.parkingName = parkingName;
        this.userIds = userIds;
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

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
