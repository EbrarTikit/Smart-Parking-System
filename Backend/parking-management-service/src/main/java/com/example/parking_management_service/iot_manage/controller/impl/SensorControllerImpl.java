package com.example.parking_management_service.iot_manage.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.iot_manage.controller.ISensorController;
import com.example.parking_management_service.iot_manage.dto.SensorDTO;
import com.example.parking_management_service.iot_manage.service.ISensorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/iot/sensors")
@Tag(name = "Sensor Controller", description = "Endpoints for managing sensors")
public class SensorControllerImpl implements ISensorController {

    @Autowired
    private ISensorService sensorService;
    
    @Override
    @PostMapping("/add")
    @Operation(
        summary = "Add a new sensor", 
        description = "Creates a new sensor in the system. ID will be automatically generated as a combination of parkingId + controllerId + echoPin + trigPin",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SensorDTO.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"parkingId\": \"1\",\n" +
                           "  \"controllerId\": \"1\",\n" +
                           "  \"echoPin\": 36,\n" +
                           "  \"trigPin\": 23\n" +
                           "}"
                )
            )
        )
    )
    public ResponseEntity<SensorDTO> addSensor(@RequestBody SensorDTO sensorDTO) {
        return new ResponseEntity<>(sensorService.addSensor(sensorDTO), HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/update")
    @Operation(
        summary = "Update a sensor", 
        description = "Updates an existing sensor in the system. ID will be recalculated based on the provided components.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"parkingId\": \"1\",\n" +
                           "  \"controllerId\": \"1\",\n" +
                           "  \"echoPin\": 36,\n" +
                           "  \"trigPin\": 23\n" +
                           "}"
                )
            )
        )
    )
    public ResponseEntity<SensorDTO> updateSensor(@RequestBody SensorDTO sensorDTO) {
        return ResponseEntity.ok(sensorService.updateSensor(sensorDTO));
    }

    @Override
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a sensor", description = "Deletes a sensor by its ID")
    public ResponseEntity<Void> deleteSensor(@PathVariable String id) {
        sensorService.deleteSensor(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/get/{id}")
    @Operation(summary = "Get a sensor", description = "Retrieves a sensor by its ID")
    public ResponseEntity<SensorDTO> getSensor(@PathVariable String id) {
        return ResponseEntity.ok(sensorService.getSensor(id));
    }

    @Override
    @GetMapping("/get/all")
    @Operation(summary = "Get all sensors", description = "Retrieves all sensors in the system")
    public ResponseEntity<List<SensorDTO>> getAllSensors() {
        return ResponseEntity.ok(sensorService.getAllSensors());
    }
}