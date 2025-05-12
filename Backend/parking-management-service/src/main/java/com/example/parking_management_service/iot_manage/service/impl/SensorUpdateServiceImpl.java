package com.example.parking_management_service.iot_manage.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.iot_manage.dto.SensorUpdateDTO;
import com.example.parking_management_service.iot_manage.service.ISensorUpdateService;
import com.example.parking_management_service.parking_info.dto.SensorUpdateDto;
import com.example.parking_management_service.parking_info.model.ParkingSpot;
import com.example.parking_management_service.parking_info.repository.ParkingSpotRepository;

@Service
public class SensorUpdateServiceImpl implements ISensorUpdateService {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private WebSocketService webSocketService;
    
    @Override
    public boolean updateParkingSpotOccupancy(SensorUpdateDTO sensorData) {
        try {
            // Sensör ID'sini oluştur
            String sensorId = generateSensorId(sensorData);
            
            // Sensör ID'sine göre ilgili park yerini bul
            ParkingSpot parkingSpot = parkingSpotRepository.findBySensorId(sensorId)
                    .orElseThrow(() -> new RuntimeException("No parking spot found with sensor ID: " + sensorId));
            
            // Park yerinin doluluk durumunu güncelle
            parkingSpot.setOccupied(sensorData.isOccupied());
            
            // Veritabanına kaydet
            parkingSpotRepository.save(parkingSpot);

            SensorUpdateDto sensorUpdateDto = new SensorUpdateDto(parkingSpot.getId(),parkingSpot.getOccupied());
            webSocketService.sendParkingSpotUpdate(sensorUpdateDto);
            
            return true;
        } catch (Exception e) {
            // Hata durumunda loglama yapılabilir
            System.err.println("Error updating parking spot occupancy: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean updateParkingSpotOccupancyFromString(String sensorDataString) {
        try {
            SensorUpdateDTO dto = SensorUpdateDTO.fromString(sensorDataString);
            return updateParkingSpotOccupancy(dto);
        } catch (Exception e) {
            System.err.println("Error parsing sensor data string: " + e.getMessage());
            return false;
        }
    }
    
    // Sensör ID'sini oluşturan yardımcı metot
    private String generateSensorId(SensorUpdateDTO dto) {
        // parkingId için 4 basamaklı format uygula
        String formattedParkingId = formatId(dto.getParkingId());
        
        // controllerId için 4 basamaklı format uygula
        String formattedControllerId = formatId(dto.getControllerId());
        
        // echoPin ve trigPin değerlerini 2 basamaklı formata dönüştür
        String formattedEchoPin = String.format("%02d", dto.getEchoPin());
        String formattedTrigPin = String.format("%02d", dto.getTrigPin());
        
        // Tüm bileşenleri birleştir
        return formattedParkingId + formattedControllerId + formattedEchoPin + formattedTrigPin;
    }
    
    // ID formatını düzenleyen yardımcı metot
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
}
