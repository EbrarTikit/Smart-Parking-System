package com.example.parking_management_service.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class TimeConfig {

    @PostConstruct
    public void init() {
        // Set default timezone to Europe/Istanbul
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
    }
}
