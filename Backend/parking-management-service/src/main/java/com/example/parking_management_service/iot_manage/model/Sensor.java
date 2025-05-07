package com.example.parking_management_service.iot_manage.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="sensor")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sensor {
    
    @Id
    private String id; // otoparkId + controllerId + echoPin + trigPin
    
    @Column(name = "parking_id")
    private String parkingId; 
    
    @Column(name = "controller_id") 
    private String controllerId;

    @Column(name = "echoPin")
    private int echoPin;

    @Column(name = "trigPin")
    private int trigPin;
    
    // distance ve timestamp kaldırıldı
}
