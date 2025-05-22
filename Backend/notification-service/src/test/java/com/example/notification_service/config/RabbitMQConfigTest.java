package com.example.notification_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = RabbitMQConfig.class)
class RabbitMQConfigTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Test
    void parkingExchange_ShouldReturnCorrectExchange() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();
        
        // Act
        TopicExchange exchange = config.parkingExchange();
        
        // Assert
        assertEquals(RabbitMQConfig.EXCHANGE_NAME, exchange.getName());
    }

    @Test
    void parkingFullQueue_ShouldReturnCorrectQueue() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();
        
        // Act
        Queue queue = config.parkingFullQueue();
        
        // Assert
        assertEquals(RabbitMQConfig.PARKING_FULL_QUEUE, queue.getName());
    }

    @Test
    void parkingFullBinding_ShouldReturnCorrectBinding() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();
        Queue queue = config.parkingFullQueue();
        TopicExchange exchange = config.parkingExchange();
        
        // Act
        Binding binding = config.parkingFullBinding(queue, exchange);
        
        // Assert
        assertEquals(RabbitMQConfig.PARKING_FULL_ROUTING_KEY, binding.getRoutingKey());
        assertEquals(queue.getName(), binding.getDestination());
    }

    @Test
    void messageConverter_ShouldReturnJackson2JsonMessageConverter() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();
        
        // Act
        Jackson2JsonMessageConverter converter = config.messageConverter();
        
        // Assert
        assertNotNull(converter);
    }

    @Test
    void rabbitTemplate_ShouldReturnConfiguredRabbitTemplate() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();
        Jackson2JsonMessageConverter converter = config.messageConverter();
        
        // Act
        RabbitTemplate template = config.rabbitTemplate(connectionFactory, converter);
        
        // Assert
        assertNotNull(template);
    }
}
