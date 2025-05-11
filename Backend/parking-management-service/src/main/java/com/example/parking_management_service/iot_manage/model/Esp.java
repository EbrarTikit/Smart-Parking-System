package com.example.parking_management_service.iot_manage.model;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="esp")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Esp {
    @Id
    private String id;
    
    // Statik sabitler (bunlar veritabanına kaydedilmez)
    @Transient
    public static final List<Integer> DEFAULT_ECHO_PINS = Arrays.asList(36, 39, 34, 35, 32, 33, 25, 26, 27, 14, 12, 13);
    
    @Transient
    public static final List<Integer> DEFAULT_TRIGGER_PINS = Arrays.asList(23, 22, 21, 19, 18, 5, 17, 16, 4, 0, 2, 15);
    
    // Veritabanına kaydedilecek alanlar
    @Column(name = "echo_pins", columnDefinition = "text")
    private String echoPinsJson; // Json formatında saklanacak
    
    @Column(name = "trigger_pins", columnDefinition = "text")
    private String triggerPinsJson; // Json formatında saklanacak
    
    // Bu alanlar veritabanına kaydedilmeyecek ancak Java kodu içinde kullanılacak
    @Transient
    private List<Integer> echoPins;
    
    @Transient
    private List<Integer> triggerPins;
}