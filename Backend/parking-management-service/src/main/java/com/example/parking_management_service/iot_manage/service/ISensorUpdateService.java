package com.example.parking_management_service.iot_manage.service;

import com.example.parking_management_service.iot_manage.dto.SensorUpdateDTO;

public interface ISensorUpdateService {
    boolean updateParkingSpotOccupancy(SensorUpdateDTO sensorData);
    boolean updateParkingSpotOccupancyFromString(String sensorDataString);
}
