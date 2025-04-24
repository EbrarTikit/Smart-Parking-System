package com.example.user_service.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("user-service")
                .packagesToScan("com.example.user_service.controller")  
                .pathsToMatch("/api/**")  // API endpoint'lerinin yolları
                .build();
    }
}