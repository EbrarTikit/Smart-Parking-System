package com.example.parking_management_service.iot_manage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.iot_manage.model.ParkingSlot;

@Repository
public interface IParkingSlotRepository extends JpaRepository<ParkingSlot,Long>{

}
