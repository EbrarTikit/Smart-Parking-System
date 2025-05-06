package com.example.parking_management_service.user_density.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.parking_management_service.user_density.model.ParkingViewer;

@Repository
public interface ParkingViewerRepository extends JpaRepository<ParkingViewer, Long> {

    @Query("SELECT COUNT(pv) FROM ParkingViewer pv WHERE pv.parkingId = :parkingId AND pv.expiryTime > :now")
    Long countActiveViewersByParkingId(Long parkingId, LocalDateTime now);

    List<ParkingViewer> findByParkingIdAndExpiryTimeGreaterThan(Long parkingId, LocalDateTime now);

    Optional<ParkingViewer> findByUserIdAndParkingId(Long userId, Long parkingId);

    List<ParkingViewer> findByExpiryTimeLessThan(LocalDateTime now);

    @Query("SELECT pv FROM ParkingViewer pv WHERE pv.parkingId = :parkingId AND pv.expiryTime > :now AND pv.notificationSent = false")
    List<ParkingViewer> findActiveNonNotifiedViewersByParkingId(Long parkingId, LocalDateTime now);
}
