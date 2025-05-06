// Backend/parking-management-service/src/main/java/com/example/parking_management_service/iot_manage/repository/ISensorRepository.java
package com.example.parking_management_service.iot_manage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.iot_manage.model.Sensor;

@Repository
public interface ISensorRepository extends JpaRepository<Sensor, String> {
}
