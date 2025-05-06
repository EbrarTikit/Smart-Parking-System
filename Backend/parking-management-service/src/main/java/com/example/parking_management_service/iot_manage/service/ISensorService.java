package com.example.parking_management_service.iot_manage.service;

import java.util.List;

import com.example.parking_management_service.iot_manage.dto.SensorDTO;


public interface ISensorService {
    SensorDTO addSensor(SensorDTO sensorDTO);
    SensorDTO updateSensor(SensorDTO sensorDTO);
    void deleteSensor(String id);
    SensorDTO getSensor(String id);
    List<SensorDTO> getAllSensors();
}
