package com.example.parking_management_service.iot_manage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.iot_manage.model.Sensor;

@Repository
public interface IIoTSensorRepository extends JpaRepository<Sensor,Long>{

}
