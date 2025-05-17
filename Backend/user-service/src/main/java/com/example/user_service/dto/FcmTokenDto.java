package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;


public class FcmTokenDto {
    @NotBlank
    private String token;
    
    @NotBlank
    private String deviceId;

    public FcmTokenDto(String deviceId, String token) {
        this.deviceId = deviceId;
        this.token = token;
    }

    public FcmTokenDto() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


}