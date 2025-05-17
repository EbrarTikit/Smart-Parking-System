package com.example.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.user_service.model.FcmToken;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserIdAndActiveTrue(Long userId);
    Optional<FcmToken> findByTokenAndDeviceId(String token, String deviceId);
}