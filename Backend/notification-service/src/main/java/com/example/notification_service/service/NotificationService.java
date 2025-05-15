package com.example.notification_service.service;

import com.example.notification_service.model.ParkingFullNotification;
import com.example.notification_service.dto.NotificationPreferencesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final WebClient userServiceClient;
    
    @Autowired
    public NotificationService(WebClient.Builder webClientBuilder) {
        this.userServiceClient = webClientBuilder.baseUrl("http://user-service:8050").build();
    }
    
    public void processParkingFullNotification(ParkingFullNotification notification) {
        logger.info("Processing parking full notification for parking: {}", notification.getParkingName());
        
        notification.getUserIds().forEach(userId -> {
            // Check if user has enabled notifications
            boolean hasNotificationsEnabled = checkUserNotificationPreference(userId);
            
            if (hasNotificationsEnabled) {
                sendMobileNotification(userId, notification.getParkingName());
            } else {
                logger.info("User {} has not enabled parking full notifications", userId);
            }
        });
    }
    
    private boolean checkUserNotificationPreference(Long userId) {
        try {
            return userServiceClient.get()
                .uri("/api/users/{userId}/notification-preferences", userId)
                .retrieve()
                .bodyToMono(NotificationPreferencesDto.class)
                .map(prefs -> prefs.isParkingFullNotification())
                .block();
        } catch (Exception e) {
            logger.error("Error checking notification preference for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    private void sendMobileNotification(Long userId, String parkingName) {
        // In a real implementation, this would use Firebase Cloud Messaging or another mobile push notification service
        logger.info("Sending mobile notification to user {} about parking {} being full", userId, parkingName);
        
        // Simulate sending a notification
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "Parking Full Alert");
        notificationData.put("body", "The parking " + parkingName + " is now full");
        notificationData.put("userId", userId);
        
        logger.info("Mobile notification sent: {}", notificationData);
    }
}
