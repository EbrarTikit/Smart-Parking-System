package com.example.parking_management_service.user_density.dto;

public class ViewerCountDTO {
    private Long parkingId;
    private Long viewerCount;

    public ViewerCountDTO() {
    }

    public ViewerCountDTO(Long parkingId, Long viewerCount) {
        this.parkingId = parkingId;
        this.viewerCount = viewerCount;
    }

    public Long getParkingId() {
        return parkingId;
    }

    public void setParkingId(Long parkingId) {
        this.parkingId = parkingId;
    }

    public Long getViewerCount() {
        return viewerCount;
    }

    public void setViewerCount(Long viewerCount) {
        this.viewerCount = viewerCount;
    }
} 