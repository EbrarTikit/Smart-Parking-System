package com.example.notification_service.service;

import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.model.ParkingFullNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQListener {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    
    private final NotificationService notificationService;
    
    @Autowired
    public RabbitMQListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = RabbitMQConfig.PARKING_FULL_QUEUE)
    public void handleParkingFullNotification(ParkingFullNotification notification) {
        logger.info("Received parking full notification for parking: {}", notification.getParkingName());
        notificationService.processParkingFullNotification(notification);
    }
}
