package com.example.navigation_service.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = "com.example.navigation_service")
@EnableJpaRepositories(basePackages = "com.example.navigation_service.repository")
@EntityScan(basePackages = "com.example.navigation_service.model")
public class NavigationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NavigationServiceApplication.class, args);
	}

}
