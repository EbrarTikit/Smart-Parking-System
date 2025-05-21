package com.example.notification_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class RabbitMQListenerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void rabbitMQListener_ShouldBeCreated() {
        // Act
        RabbitMQListener rabbitMQListener = new RabbitMQListener(notificationService);
        
        // Assert
        assertNotNull(rabbitMQListener);
    }
}
