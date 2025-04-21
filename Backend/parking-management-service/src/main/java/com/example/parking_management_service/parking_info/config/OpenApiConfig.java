package com.example.parking_management_service.parking_info.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parking Management API")
                        .version("1.0")
                        .description("API for managing parking lots including details such as name, location, capacity, opening/closing hours, and rates.")
                        .contact(new Contact().name("Smart Parking Team"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", 
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")));
    }
} 