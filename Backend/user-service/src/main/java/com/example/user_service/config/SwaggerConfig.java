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
                .packagesToScan("com.example.user_service.controller")  // API controller'larınızın bulunduğu paketi belirtin
                .pathsToMatch("/api/**")  // API endpoint'lerinizin yollarını belirtin
                .build();
    }
}