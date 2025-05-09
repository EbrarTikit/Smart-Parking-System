package com.example.user_service.exception;

public class ParkingServiceException extends RuntimeException {
    public ParkingServiceException(String message) {
        super(message);
    }

    public ParkingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
