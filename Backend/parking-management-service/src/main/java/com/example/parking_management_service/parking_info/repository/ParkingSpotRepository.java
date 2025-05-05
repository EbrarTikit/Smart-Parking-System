package com.example.parking_management_service.parking_info.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.parking_info.model.ParkingSpot;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    
    List<ParkingSpot> findByParkingId(Long parkingId);
    
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.parking.id = :parkingId AND ps.row = :row AND ps.column = :column")
    ParkingSpot findByParkingIdAndRowAndColumn(
        @Param("parkingId") Long parkingId, 
        @Param("row") int row, 
        @Param("column") int column
    );
}
