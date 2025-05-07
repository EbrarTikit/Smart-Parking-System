package com.example.parking_management_service.iot_manage.controller.impl;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.parking_management_service.iot_manage.controller.IEspController;
import com.example.parking_management_service.iot_manage.dto.EspDTO;
import com.example.parking_management_service.iot_manage.service.IEspService;
import com.example.parking_management_service.iot_manage.model.Esp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/iot/esps")
@Tag(name = "ESP Controller", description = "Endpoints for managing ESP devices")
public class EspControllerImpl implements IEspController {

    @Autowired
    private IEspService espService;
    
    @Override
    @PostMapping("/add")
    @Operation(
    summary = "Add a new ESP device", 
    description = "Creates a new ESP device in the system. ID will be automatically formatted to 4 digits (e.g. '1' becomes '0001'). Echo and trigger pins are predefined and cannot be modified.",
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EspDTO.class),
            examples = @ExampleObject(
                value = "{\n  \"id\": \"1\"\n}"
            )
        )
    )
    )
    public ResponseEntity<EspDTO> addEsp(@RequestBody EspDTO espDTO) {
        // Kullanıcı echoPins ve triggerPins değerlerini belirtmiş olabilir, 
        // bu durumda bunları null yaparak varsayılan değerlerin kullanılmasını sağlıyoruz
        espDTO.setEchoPins(null);
        espDTO.setTriggerPins(null);
        
        return new ResponseEntity<>(espService.addEsp(espDTO), HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/update")
    @Operation(summary = "Update an ESP device", description = "Updates an existing ESP device in the system")
    public ResponseEntity<EspDTO> updateEsp(EspDTO espDTO) {
        return ResponseEntity.ok(espService.updateEsp(espDTO));
    }

    @Override
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete an ESP device", description = "Deletes an ESP device by its ID")
    public ResponseEntity<Void> deleteEsp(String id) {
        espService.deleteEsp(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/get/{id}")
    @Operation(summary = "Get an ESP device", description = "Retrieves an ESP device by its ID")
    public ResponseEntity<EspDTO> getEsp(String id) {
        return ResponseEntity.ok(espService.getEsp(id));
    }

    @Override
    @GetMapping("/get/all")
    @Operation(summary = "Get all ESP devices", description = "Retrieves all ESP devices in the system")
    public ResponseEntity<List<EspDTO>> getAllEsps() {
        return ResponseEntity.ok(espService.getAllEsps());
    }

    @GetMapping("/pins")
    @Operation(summary = "Get ESP pin configuration", description = "Returns the fixed pin configuration for ESP devices")
    public ResponseEntity<Map<String, List<Integer>>> getPinConfiguration() {
        Map<String, List<Integer>> pinConfig = new HashMap<>();
        // Değişken adlarını DEFAULT_ECHO_PINS ve DEFAULT_TRIGGER_PINS olarak güncelle
        pinConfig.put("echoPins", Esp.DEFAULT_ECHO_PINS);
        pinConfig.put("triggerPins", Esp.DEFAULT_TRIGGER_PINS);
        return ResponseEntity.ok(pinConfig);
    }
}