// Backend/parking-management-service/src/main/java/com/example/parking_management_service/iot_manage/repository/IControllerRepository.java
package com.example.parking_management_service.iot_manage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.iot_manage.model.Esp;

@Repository
public interface IEspRepository extends JpaRepository<Esp, String> {
}