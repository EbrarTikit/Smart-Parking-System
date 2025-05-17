package com.example.notification_service.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);
    
    public void sendNotification(String token, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent message: {}", response);
        } catch (Exception e) {
            logger.error("Failed to send Firebase notification", e);
        }
    }
    
    public void sendNotificationToTopic(String topic, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent message to topic {}: {}", topic, response);
        } catch (Exception e) {
            logger.error("Failed to send Firebase notification to topic", e);
        }
    }
}
