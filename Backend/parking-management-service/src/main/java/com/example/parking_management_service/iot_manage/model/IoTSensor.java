package com.example.parking_management_service.iot_manage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="iot_sensors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IoTSensor {
    @Id  // Bu alanın birincil anahtar olduğunu belirtir
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID'nin otomatik olarak artırılacağını belirtir
    private Long id;

    @Column(name = "echoPin")
    private int echo;

    @Column(name = "trigPin")
    private int trig;

    @Column(name = "distance")
    private int distance;

    @Column(name = "last_connection")
    private LocalDateTime timeStamp;  // Son bağlantı zamanı
    
}
