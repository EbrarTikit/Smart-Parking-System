package com.example.parking_management_service.iot_manage.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.iot_manage.dto.SensorDTO;
import com.example.parking_management_service.iot_manage.model.Sensor;
import com.example.parking_management_service.iot_manage.repository.ISensorRepository;
import com.example.parking_management_service.iot_manage.service.ISensorService;

@Service
public class SensorServiceImpl implements ISensorService {

    @Autowired
    private ISensorRepository sensorRepository;
    
    @Override
    public SensorDTO addSensor(SensorDTO sensorDTO) {
        // ID'yi oluştur: parkingId + controllerId + echoPin + trigPin
        String generatedId = generateSensorId(sensorDTO);
        sensorDTO.setId(generatedId);
        
        Sensor sensor = mapToSensorEntity(sensorDTO);
        sensor = sensorRepository.save(sensor);
        return mapToSensorDTO(sensor);
    }

    @Override
    public SensorDTO updateSensor(SensorDTO sensorDTO) {
        // ID'yi tekrar oluştur (güncelleme sırasında bileşenleri değişmiş olabilir)
        String generatedId = generateSensorId(sensorDTO);
        sensorDTO.setId(generatedId);
        
        if(sensorRepository.existsById(sensorDTO.getId())) {
            Sensor sensor = mapToSensorEntity(sensorDTO);
            sensor = sensorRepository.save(sensor);
            return mapToSensorDTO(sensor);
        }
        throw new RuntimeException("Sensor not found with id: " + sensorDTO.getId());
    }

    @Override
    public void deleteSensor(String id) {
        sensorRepository.deleteById(id);
    }

    @Override
    public SensorDTO getSensor(String id) {
        Sensor sensor = sensorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sensor not found with id: " + id));
        return mapToSensorDTO(sensor);
    }

    @Override
    public List<SensorDTO> getAllSensors() {
        return sensorRepository.findAll().stream()
            .map(this::mapToSensorDTO)
            .collect(Collectors.toList());
    }
    
    // Sensor ID oluşturma yardımcı metodu
    private String generateSensorId(SensorDTO dto) {
        // parkingId için 4 basamaklı format uygula (ESP ID ile aynı mantık)
        String formattedParkingId = formatId(dto.getParkingId());
        
        // controllerId için 4 basamaklı format uygula
        String formattedControllerId = formatId(dto.getControllerId());
        
        // echoPin ve trigPin değerlerini 2 basamaklı formata dönüştür
        String formattedEchoPin = String.format("%02d", dto.getEchoPin());
        String formattedTrigPin = String.format("%02d", dto.getTrigPin());
        
        // Tüm bileşenleri birleştir
        return formattedParkingId + formattedControllerId + formattedEchoPin + formattedTrigPin;
    }
    
    // ID formatını düzenleyen yardımcı metot (EspServiceImpl'den alındı)
    private String formatId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty");
        }
        
        try {
            // ID'yi sayıya çevir
            int numericId = Integer.parseInt(id);
            
            // 4 basamaklı formata dönüştür
            return String.format("%04d", numericId);
        } catch (NumberFormatException e) {
            // Eğer sayısal değilse, orijinal ID'yi döndür
            return id;
        }
    }
    
    // Yardımcı dönüşüm metotları
    private Sensor mapToSensorEntity(SensorDTO dto) {
        Sensor sensor = new Sensor();
        sensor.setId(dto.getId());
        sensor.setParkingId(dto.getParkingId());
        sensor.setControllerId(dto.getControllerId());
        sensor.setEchoPin(dto.getEchoPin());
        sensor.setTrigPin(dto.getTrigPin());
        return sensor;
    }
    
    private SensorDTO mapToSensorDTO(Sensor entity) {
        return new SensorDTO(
            entity.getId(),
            entity.getParkingId(),
            entity.getControllerId(),
            entity.getEchoPin(),
            entity.getTrigPin()
        );
    }
}