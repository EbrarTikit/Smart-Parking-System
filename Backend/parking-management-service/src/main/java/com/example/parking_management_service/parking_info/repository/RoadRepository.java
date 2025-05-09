package com.example.parking_management_service.parking_info.repository;

import com.example.parking_management_service.parking_info.model.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadRepository extends JpaRepository<Road, Long> {
}
