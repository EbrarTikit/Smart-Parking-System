package com.example.parking_management_service.iot_manage.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.parking_management_service.iot_manage.dto.SensorDTO;

public interface ISensorController {
    
    @PostMapping
    ResponseEntity<SensorDTO> addSensor(@RequestBody SensorDTO sensorDTO);
    
    @PutMapping
    ResponseEntity<SensorDTO> updateSensor(@RequestBody SensorDTO sensorDTO);
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteSensor(@PathVariable String id);
    
    @GetMapping("/{id}")
    ResponseEntity<SensorDTO> getSensor(@PathVariable String id);
    
    @GetMapping
    ResponseEntity<List<SensorDTO>> getAllSensors();
}