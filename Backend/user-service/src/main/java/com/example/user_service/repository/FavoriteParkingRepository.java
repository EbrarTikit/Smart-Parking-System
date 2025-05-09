package com.example.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_service.model.FavoriteParking;

public interface FavoriteParkingRepository extends JpaRepository<FavoriteParking, Long> {
    List<FavoriteParking> findByUserId(Long userId);
    Optional<FavoriteParking> findByUserIdAndParkingId(Long userId, Long parkingId);
    boolean existsByUserIdAndParkingId(Long userId, Long parkingId);
}