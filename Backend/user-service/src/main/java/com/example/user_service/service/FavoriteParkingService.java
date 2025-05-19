package com.example.user_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.user_service.dto.FavoriteParkingResponseDto;
import com.example.user_service.dto.ParkingDetailsDto;
import com.example.user_service.exception.ParkingServiceException;
import com.example.user_service.model.FavoriteParking;
import com.example.user_service.model.User;
import com.example.user_service.repository.FavoriteParkingRepository;
import com.example.user_service.repository.UserRepository;

@Service
public class FavoriteParkingService {
    private static final Logger logger = LoggerFactory.getLogger(FavoriteParkingService.class);
    
    private final FavoriteParkingRepository favoriteParkingRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    
    @Value("${parking.management.service.url:http://parking-management-service:8081}")
    private String parkingServiceUrl;

    public FavoriteParkingService(FavoriteParkingRepository favoriteParkingRepository,
                                UserRepository userRepository,
                                RestTemplate restTemplate) {
        this.favoriteParkingRepository = favoriteParkingRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    public void addToFavorites(Long userId, Long parkingId) {
        logger.info("Adding favorite parking - userId: {}, parkingId: {}", userId, parkingId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        logger.info("Found user: {}", user != null ? user.getId() : "null");
        
        // Otopark var mı kontrol et
        try {
            logger.info("Fetching parking details from management service for parkingId: {}", parkingId);
            ResponseEntity<ParkingDetailsDto> response = restTemplate.getForEntity(
                parkingServiceUrl + "/api/parkings/" + parkingId,
                ParkingDetailsDto.class
            );
            logger.info("Parking management service response: {}", response.getStatusCode());
            logger.info("Parking details received: {}", response.getBody());
            
            ParkingDetailsDto parkingDetails = response.getBody();
            
            if (parkingDetails == null) {
                throw new ParkingServiceException("Parking not found with id: " + parkingId);
            }

            if (!favoriteParkingRepository.existsByUserIdAndParkingId(userId, parkingId)) {
                FavoriteParking favoriteParking = new FavoriteParking(user, parkingId);
                favoriteParkingRepository.save(favoriteParking);
                logger.info("Favorite parking saved successfully");
            }
        } catch (RestClientException e) {
            logger.error("Error fetching parking details: ", e);
            throw new ParkingServiceException("Error communicating with parking service", e);
        } catch (Exception e) {
            throw new ParkingServiceException("Unexpected error while processing request", e);
        }
    }

    public void removeFromFavorites(Long userId, Long parkingId) {
        favoriteParkingRepository.findByUserIdAndParkingId(userId, parkingId)
            .ifPresent(favoriteParkingRepository::delete);
    }

    public List<FavoriteParkingResponseDto> getFavorites(Long userId) {
        List<FavoriteParking> favorites = favoriteParkingRepository.findByUserId(userId);
        
        return favorites.stream()
            .map(favorite -> {
                try {
                    ParkingDetailsDto parkingDetails = restTemplate.getForObject(
                        parkingServiceUrl + "/api/parkings/" + favorite.getParkingId(),
                        ParkingDetailsDto.class
                    );
                    
                    return new FavoriteParkingResponseDto(
                        parkingDetails.getId(),
                        parkingDetails.getName(),
                        parkingDetails.getLocation(),
                        parkingDetails.getImageUrl(),
                        parkingDetails.getRate()
                    );
                } catch (Exception e) {
                    logger.error("Error fetching parking details: ", e);
                    return null;
                }
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
}
