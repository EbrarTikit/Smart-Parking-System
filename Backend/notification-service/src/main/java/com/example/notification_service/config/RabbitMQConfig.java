package com.example.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "parking-exchange";
    public static final String PARKING_FULL_QUEUE = "parking-full-queue";
    public static final String PARKING_FULL_ROUTING_KEY = "parking.full";

    @Bean
    public TopicExchange parkingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue parkingFullQueue() {
        return new Queue(PARKING_FULL_QUEUE);
    }

    @Bean
    public Binding parkingFullBinding(Queue parkingFullQueue, TopicExchange parkingExchange) {
        return BindingBuilder.bind(parkingFullQueue).to(parkingExchange).with(PARKING_FULL_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
