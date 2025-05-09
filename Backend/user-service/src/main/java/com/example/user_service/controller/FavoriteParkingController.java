package com.example.user_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.FavoriteParkingResponseDto;
import com.example.user_service.service.FavoriteParkingService;

@RestController
@RequestMapping("/api/users/{userId}/favorites")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FavoriteParkingController {
    private final FavoriteParkingService favoriteParkingService;

    public FavoriteParkingController(FavoriteParkingService favoriteParkingService) {
        this.favoriteParkingService = favoriteParkingService;
    }

    @GetMapping
    public ResponseEntity<List<FavoriteParkingResponseDto>> getFavorites(@PathVariable Long userId) {
        List<FavoriteParkingResponseDto> favorites = favoriteParkingService.getFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/{parkingId}")
    public ResponseEntity<Void> addToFavorites(
            @PathVariable Long userId,
            @PathVariable Long parkingId) {
        favoriteParkingService.addToFavorites(userId, parkingId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{parkingId}")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long userId,
            @PathVariable Long parkingId) {
        favoriteParkingService.removeFromFavorites(userId, parkingId);
        return ResponseEntity.ok().build();
    }
}
