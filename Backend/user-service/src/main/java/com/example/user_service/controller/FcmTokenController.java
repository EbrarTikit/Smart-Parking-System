package com.example.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.FcmTokenDto;
import com.example.user_service.service.FcmTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/{userId}/fcm")
@CrossOrigin(origins = "*")
public class FcmTokenController {
    
    private final FcmTokenService fcmTokenService;
    
    @Autowired
    public FcmTokenController(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<Void> registerToken(
            @PathVariable Long userId,
            @Valid @RequestBody FcmTokenDto tokenDto) {
        fcmTokenService.registerToken(userId, tokenDto);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/deactivate/{deviceId}")
    public ResponseEntity<Void> deactivateToken(
            @PathVariable Long userId,
            @PathVariable String deviceId) {
        fcmTokenService.deactivateToken(userId, deviceId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/token")
    public ResponseEntity<String> getUserToken(@PathVariable Long userId) {
        String token = fcmTokenService.getLatestUserToken(userId);
        if (token != null) {
            return ResponseEntity.ok(token);
        }
        return ResponseEntity.notFound().build();
    }
}
