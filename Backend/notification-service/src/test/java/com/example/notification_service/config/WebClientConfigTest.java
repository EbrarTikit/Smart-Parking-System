package com.example.notification_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebClientConfigTest {

    @Test
    void webClientBuilder_ShouldReturnWebClientBuilder() {
        // Arrange
        WebClientConfig config = new WebClientConfig();
        
        // Act
        WebClient.Builder builder = config.webClientBuilder();
        
        // Assert
        assertNotNull(builder);
    }
}
