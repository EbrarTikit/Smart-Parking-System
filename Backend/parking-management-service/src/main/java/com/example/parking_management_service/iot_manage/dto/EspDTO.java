package com.example.parking_management_service.iot_manage.dto;

import java.util.List;

import com.example.parking_management_service.iot_manage.model.Esp;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EspDTO {
    private String id;
    
    // JsonProperty(Access.READ_ONLY) - Sadece yanıtlarda gösterilir, isteklerde kabul edilmez
    // Schema(accessMode = Schema.AccessMode.READ_ONLY) - Swagger UI'da salt okunur olarak işaretler
    @JsonProperty(access = Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private List<Integer> echoPins;
    
    @JsonProperty(access = Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private List<Integer> triggerPins;
    
    // Tek parametre alan constructor
    public EspDTO(String id) {
        this.id = id;
        this.echoPins = Esp.DEFAULT_ECHO_PINS;
        this.triggerPins = Esp.DEFAULT_TRIGGER_PINS;
    }
}