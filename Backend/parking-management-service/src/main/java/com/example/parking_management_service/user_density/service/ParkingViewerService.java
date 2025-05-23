package com.example.parking_management_service.user_density.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.user_density.dto.ViewerCountDTO;
import com.example.parking_management_service.user_density.model.ParkingViewer;
import com.example.parking_management_service.user_density.repository.ParkingViewerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ParkingViewerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ParkingViewerService.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Istanbul");

    @Autowired
    private ParkingViewerRepository parkingViewerRepository;

    @Autowired
    private ParkingRepository parkingRepository;
    
    @Autowired
    private NotificationSenderService notificationSenderService;

    // Track when a user views a parking
    public ViewerCountDTO trackUserViewing(Long userId, Long parkingId) {
        // Use ZonedDateTime to ensure correct timezone
        ZonedDateTime zonedNow = ZonedDateTime.now(ZONE_ID);
        LocalDateTime now = zonedNow.toLocalDateTime();
        
        // Add 45 minutes (average between 30-60 minutes)
        LocalDateTime expiryTime = now.plusMinutes(45);

        // Check if the user is already viewing this parking
        Optional<ParkingViewer> existingViewer = parkingViewerRepository.findByUserIdAndParkingId(userId, parkingId);

        if (existingViewer.isPresent()) {
            // Update the existing record
            ParkingViewer viewer = existingViewer.get();
            viewer.setViewingStartTime(now);
            viewer.setExpiryTime(expiryTime);
            viewer.setNotificationSent(false);
            parkingViewerRepository.save(viewer);
        } else {
            // Create a new record
            ParkingViewer newViewer = new ParkingViewer(userId, parkingId, now, expiryTime);
            parkingViewerRepository.save(newViewer);
        }

        // Return the updated viewer count
        Long viewerCount = parkingViewerRepository.countActiveViewersByParkingId(parkingId, now);
        return new ViewerCountDTO(parkingId, viewerCount);
    }

    // Get the current viewer count for a parking
    public ViewerCountDTO getViewerCount(Long parkingId) {
        // Use ZonedDateTime to ensure correct timezone
        ZonedDateTime zonedNow = ZonedDateTime.now(ZONE_ID);
        LocalDateTime now = zonedNow.toLocalDateTime();
        
        Long viewerCount = parkingViewerRepository.countActiveViewersByParkingId(parkingId, now);
        return new ViewerCountDTO(parkingId, viewerCount);
    }

    // Clean up expired viewers
    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    public void cleanupExpiredViewers() {
        // Use ZonedDateTime to ensure correct timezone
        ZonedDateTime zonedNow = ZonedDateTime.now(ZONE_ID);
        LocalDateTime now = zonedNow.toLocalDateTime();
        
        List<ParkingViewer> expiredViewers = parkingViewerRepository.findByExpiryTimeLessThan(now);
        parkingViewerRepository.deleteAll(expiredViewers);
    }

    // Get list of users who should be notified when parking becomes full
    public List<ParkingViewer> getUsersToNotifyForFullParking(Long parkingId) {
        // Use ZonedDateTime to ensure correct timezone
        ZonedDateTime zonedNow = ZonedDateTime.now(ZONE_ID);
        LocalDateTime now = zonedNow.toLocalDateTime();
        
        return parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(parkingId, now);
    }
    
    // Mark user as notified
    public void markUserAsNotified(Long viewerId) {
        parkingViewerRepository.findById(viewerId).ifPresent(viewer -> {
            viewer.setNotificationSent(true);
            parkingViewerRepository.save(viewer);
        });
    }
    
    // Check if a parking is full
    public boolean isParkingFull(Long parkingId) {
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);
        
        if (parkingOpt.isPresent()) {
            Parking parking = parkingOpt.get();
            long occupiedSpots = parking.getParkingSpots().stream()
                .filter(spot -> !spot.isAvailable())
                .count();
                
            return occupiedSpots >= parking.getCapacity();
        }
        
        return false;
    }
    
    // This method now uses the notification sender service
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkParkingStatusForNotifications() {
        logger.info("Starting scheduled notification check");
        List<Parking> parkings = parkingRepository.findAll();
        logger.info("Found {} parkings to check", parkings.size());
        
        for (Parking parking : parkings) {
            logger.info("Checking parking: {} (ID: {})", parking.getName(), parking.getId());
            boolean isFull = isParkingFull(parking.getId());
            logger.info("Parking {} is full: {}", parking.getId(), isFull);
            
            if (isFull) {
                logger.info("Sending notifications for parking {}", parking.getId());
                notificationSenderService.sendParkingFullNotification(parking.getId());
            }
        }
        logger.info("Completed scheduled notification check");
    }
}
