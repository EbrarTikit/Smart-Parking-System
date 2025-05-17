package com.example.user_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.FcmTokenDto;
import com.example.user_service.model.FcmToken;
import com.example.user_service.repository.FcmTokenRepository;

@Service
public class FcmTokenService {
    
    private final FcmTokenRepository fcmTokenRepository;
    
    @Autowired
    public FcmTokenService(FcmTokenRepository fcmTokenRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
    }
    
    public void registerToken(Long userId, FcmTokenDto tokenDto) {
        // Check if token already exists for this device
        Optional<FcmToken> existingToken = fcmTokenRepository.findByTokenAndDeviceId(
            tokenDto.getToken(), tokenDto.getDeviceId());
        
        if (existingToken.isPresent()) {
            FcmToken token = existingToken.get();
            // Update user ID if needed
            if (!token.getUserId().equals(userId)) {
                token.setUserId(userId);
                token.setActive(true);
                fcmTokenRepository.save(token);
            }
        } else {
            // Create new token
            FcmToken newToken = new FcmToken();
            newToken.setUserId(userId);
            newToken.setToken(tokenDto.getToken());
            newToken.setDeviceId(tokenDto.getDeviceId());
            newToken.setActive(true);
            fcmTokenRepository.save(newToken);
        }
    }
    
    public void deactivateToken(Long userId, String deviceId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndActiveTrue(userId);
        for (FcmToken token : tokens) {
            if (token.getDeviceId().equals(deviceId)) {
                token.setActive(false);
                fcmTokenRepository.save(token);
                break;
            }
        }
    }
    
    public List<FcmToken> getUserActiveTokens(Long userId) {
        return fcmTokenRepository.findByUserIdAndActiveTrue(userId);
    }
    
    public String getLatestUserToken(Long userId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndActiveTrue(userId);
        if (!tokens.isEmpty()) {
            return tokens.get(0).getToken();
        }
        return null;
    }
}