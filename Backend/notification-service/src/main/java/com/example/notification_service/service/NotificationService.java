package com.example.notification_service.service;

import com.example.notification_service.model.ParkingFullNotification;
import com.example.notification_service.dto.NotificationPreferencesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final WebClient userServiceClient;
    private final FirebaseNotificationService firebaseNotificationService;
    
    @Autowired
    public NotificationService(WebClient.Builder webClientBuilder, FirebaseNotificationService firebaseNotificationService) {
        this.userServiceClient = webClientBuilder.baseUrl("http://user-service:8050").build();
        this.firebaseNotificationService = firebaseNotificationService;
    }
    
    public void processParkingFullNotification(ParkingFullNotification notification) {
        logger.info("Processing parking full notification for parking: {}", notification.getParkingName());
        
        // Create notification title and body
        String title = "Parking Full Alert";
        String body = "The parking " + notification.getParkingName() + " is now full.";
        
        // Send to all users who should receive this notification
        for (Long userId : notification.getUserIds()) {
            // Send to user's specific topic
            String userTopic = "user_" + userId;
            firebaseNotificationService.sendNotificationToTopic(userTopic, title, body);
        }
        
        // Also send to a general topic for this parking
        String parkingTopic = "parking_" + notification.getParkingId();
        firebaseNotificationService.sendNotificationToTopic(parkingTopic, title, body);
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
    
    private String getUserFcmToken(Long userId) {
        try {
            return userServiceClient.get()
                .uri("/api/users/{userId}/fcm/token", userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            logger.error("Error fetching FCM token for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
