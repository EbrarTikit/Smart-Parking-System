package com.example.parking_management_service.iot_manage.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.parking_management_service.iot_manage.dto.EspDTO;

public interface IEspController {
    
    @GetMapping(path = "/parking-location/{id}")
    @PostMapping
    ResponseEntity<EspDTO> addEsp(@RequestBody EspDTO espDTO);
    
    @PutMapping
    ResponseEntity<EspDTO> updateEsp(@RequestBody EspDTO espDTO);
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteEsp(@PathVariable String id);
    
    @GetMapping("/{id}")
    ResponseEntity<EspDTO> getEsp(@PathVariable String id);
    
    @GetMapping
    ResponseEntity<List<EspDTO>> getAllEsps();
}
