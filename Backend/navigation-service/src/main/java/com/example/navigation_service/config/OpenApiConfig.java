package com.example.navigation_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Park Location API")
                        .version("1.0")
                        .description("API for managing park locations")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your.email@example.com")));
    }
}