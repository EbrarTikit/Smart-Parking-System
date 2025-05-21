package com.example.parking_management_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ParkingManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkingManagementServiceApplication.class, args);
	}

}
