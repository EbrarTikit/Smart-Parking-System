package com.example.parking_management_service.iot_manage.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.iot_manage.dto.EspDTO;
import com.example.parking_management_service.iot_manage.model.Esp;
import com.example.parking_management_service.iot_manage.repository.IEspRepository;
import com.example.parking_management_service.iot_manage.service.IEspService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class EspServiceImpl implements IEspService {

    @Autowired
    private IEspRepository espRepository;
    
    @Autowired
    private ObjectMapper objectMapper; // JSON işlemleri için
    
    @Override
    public EspDTO addEsp(EspDTO espDTO) {
        // ID formatını düzelt
        espDTO.setId(formatEspId(espDTO.getId()));
    
        // Echo ve trigger pins değerlerini her zaman varsayılan değerler olarak ayarla
        espDTO.setEchoPins(Esp.DEFAULT_ECHO_PINS);
        espDTO.setTriggerPins(Esp.DEFAULT_TRIGGER_PINS);
    
        Esp esp = mapToEspEntity(espDTO);
        esp = espRepository.save(esp);
        return mapToEspDTO(esp);
    }

    @Override
    public EspDTO updateEsp(EspDTO espDTO) {
        // ID formatını düzelt
        espDTO.setId(formatEspId(espDTO.getId()));
        
        final String formattedId = espDTO.getId();
        
        if(espRepository.existsById(formattedId)) {
            Esp esp = mapToEspEntity(espDTO);
            esp = espRepository.save(esp);
            return mapToEspDTO(esp);
        }
        throw new RuntimeException("Esp not found with id: " + formattedId);
    }

    @Override
    public void deleteEsp(String id) {
        // ID formatını düzelt
        final String formattedId = formatEspId(id);
        espRepository.deleteById(formattedId);
    }

    @Override
    public EspDTO getEsp(String id) {
        // ID formatını düzelt
        final String formattedId = formatEspId(id);
        
        Esp esp = espRepository.findById(formattedId)
            .orElseThrow(() -> new RuntimeException("Esp not found with id: " + formattedId));
        return mapToEspDTO(esp);
    }

    @Override
    public List<EspDTO> getAllEsps() {
        return espRepository.findAll().stream()
            .map(this::mapToEspDTO)
            .collect(Collectors.toList());
    }
    
    // ID formatını düzenleyen yardımcı metot
    private String formatEspId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ESP ID cannot be empty");
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
    
    // Yardımcı dönüşüm metotları - JSON dönüşümü eklendi
    private Esp mapToEspEntity(EspDTO dto) {
        Esp esp = new Esp();
        esp.setId(dto.getId());
        
        try {
            // List<Integer>'ı JSON'a dönüştür
            esp.setEchoPinsJson(objectMapper.writeValueAsString(dto.getEchoPins()));
            esp.setTriggerPinsJson(objectMapper.writeValueAsString(dto.getTriggerPins()));
            
            // Transient alanlar için
            esp.setEchoPins(dto.getEchoPins());
            esp.setTriggerPins(dto.getTriggerPins());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting pins to JSON", e);
        }
        
        return esp;
    }
    
    private EspDTO mapToEspDTO(Esp entity) {
        EspDTO dto = new EspDTO();
        dto.setId(entity.getId());
        
        // JSON'dan List<Integer>'a dönüştür
        try {
            if (entity.getEchoPinsJson() != null && !entity.getEchoPinsJson().isEmpty()) {
                dto.setEchoPins(objectMapper.readValue(entity.getEchoPinsJson(), 
                                new TypeReference<List<Integer>>(){}));
            } else {
                dto.setEchoPins(Esp.DEFAULT_ECHO_PINS);
            }
            
            if (entity.getTriggerPinsJson() != null && !entity.getTriggerPinsJson().isEmpty()) {
                dto.setTriggerPins(objectMapper.readValue(entity.getTriggerPinsJson(), 
                                  new TypeReference<List<Integer>>(){}));
            } else {
                dto.setTriggerPins(Esp.DEFAULT_TRIGGER_PINS);
            }
        } catch (JsonProcessingException e) {
            // JSON dönüşümünde hata olursa varsayılanları kullan
            dto.setEchoPins(Esp.DEFAULT_ECHO_PINS);
            dto.setTriggerPins(Esp.DEFAULT_TRIGGER_PINS);
        }
        
        return dto;
    }
}