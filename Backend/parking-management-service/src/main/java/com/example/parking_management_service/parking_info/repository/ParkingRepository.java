package com.example.parking_management_service.parking_info.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.parking_info.model.Parking;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {
    
    @Override
    List<Parking> findAll();
    
    @Override
    Optional<Parking> findById(Long id);
    
    @Override
    <S extends Parking> S save(S entity); // use adding new parking or updating an existing parking
    
    @Override
    void delete(Parking entity);
} 