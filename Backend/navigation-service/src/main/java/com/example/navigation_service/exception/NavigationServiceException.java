package com.example.navigation_service.exception;

public class NavigationServiceException extends RuntimeException {
    
    public NavigationServiceException(String message) {
        super(message);
    }

    public NavigationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
} 