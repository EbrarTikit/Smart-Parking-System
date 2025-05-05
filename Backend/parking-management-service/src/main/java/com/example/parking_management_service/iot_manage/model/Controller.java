package com.example.parking_management_service.iot_manage.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="sensors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Controller {

    @Id  // Bu alanın birincil anahtar olduğunu belirtir
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID'nin otomatik olarak artırılacağını belirtir
    private Long id;

    private List<Integer> trigList;

    private List<Integer> echoList;

}
