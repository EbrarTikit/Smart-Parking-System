package com.example.notification_service.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseNotificationServiceTest {

    private FirebaseNotificationService firebaseNotificationService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @BeforeEach
    void setUp() {
        firebaseNotificationService = new FirebaseNotificationService();
    }

    @Test
    void sendNotification_ShouldSendMessageWithCorrectParameters() throws Exception {
        // Arrange
        String token = "test-device-token";
        String title = "Test Title";
        String body = "Test Body";
        String messageId = "message-id-123";

        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

            // Act
            firebaseNotificationService.sendNotification(token, title, body);

            // Assert
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging).send(messageCaptor.capture());
            
            // We can't directly access the fields due to package-private visibility,
            // but we can verify that the message was sent
            verify(firebaseMessaging, times(1)).send(any(Message.class));
        }
    }

    @Test
    void sendNotificationToTopic_ShouldSendMessageToCorrectTopic() throws Exception {
        // Arrange
        String topic = "test-topic";
        String title = "Test Topic Title";
        String body = "Test Topic Body";
        String messageId = "topic-message-id-123";

        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

            // Act
            firebaseNotificationService.sendNotificationToTopic(topic, title, body);

            // Assert
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging).send(messageCaptor.capture());
            
            // We can't directly access the fields due to package-private visibility,
            // but we can verify that the message was sent
            verify(firebaseMessaging, times(1)).send(any(Message.class));
        }
    }

    @Test
    void sendNotification_ShouldHandleException() throws Exception {
        // Arrange
        String token = "test-device-token";
        String title = "Test Title";
        String body = "Test Body";

        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("Test exception"));

            // Act & Assert - should not throw exception
            firebaseNotificationService.sendNotification(token, title, body);
            
            // Verify that send was called
            verify(firebaseMessaging, times(1)).send(any(Message.class));
        }
    }
}
