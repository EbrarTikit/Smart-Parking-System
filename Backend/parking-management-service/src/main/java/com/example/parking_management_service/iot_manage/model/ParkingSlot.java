package com.example.parking_management_service.iot_manage.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="parking_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlot {
    
    @Id  // Bu alanın birincil anahtar olduğunu belirtir
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID'nin otomatik olarak artırılacağını belirtir
    private Long id;
    
    @OneToOne @JoinColumn(name="sensor_id")
    private IoTSensor sensor;
    
    @Column(name="slot_location")
    private Long slotLocation;
    
    @Column
    private boolean occupied;  // Cihazın aktif olup olmadığı

}
