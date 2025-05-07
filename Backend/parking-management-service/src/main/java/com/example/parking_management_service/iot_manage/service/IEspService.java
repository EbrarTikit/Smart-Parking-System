package com.example.parking_management_service.iot_manage.service;

import java.util.List;

import com.example.parking_management_service.iot_manage.dto.EspDTO;

public interface IEspService {
    
    EspDTO addEsp(EspDTO espDTO);
    EspDTO updateEsp(EspDTO espDTO);
    void deleteEsp(String id);
    EspDTO getEsp(String id);
    List<EspDTO> getAllEsps();

}