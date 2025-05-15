package com.example.parking_management_service.user_density.service;

import com.example.parking_management_service.config.RabbitMQConfig;
import com.example.parking_management_service.parking_info.model.Parking;
import com.example.parking_management_service.parking_info.repository.ParkingRepository;
import com.example.parking_management_service.user_density.dto.ParkingFullNotificationDTO;
import com.example.parking_management_service.user_density.model.ParkingViewer;
import com.example.parking_management_service.user_density.repository.ParkingViewerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationSenderService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSenderService.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final ParkingRepository parkingRepository;
    private final ParkingViewerRepository parkingViewerRepository;
    
    @Autowired
    public NotificationSenderService(
            RabbitTemplate rabbitTemplate,
            ParkingRepository parkingRepository,
            ParkingViewerRepository parkingViewerRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.parkingRepository = parkingRepository;
        this.parkingViewerRepository = parkingViewerRepository;
    }
    
    public void sendParkingFullNotification(Long parkingId) {
        logger.info("Starting to send notification for parking {}", parkingId);
        
        // Check if parking is actually full
        if (!isParkingFull(parkingId)) {
            logger.info("Parking {} is not full, no notification needed", parkingId);
            return;
        }
        
        // Get users to notify
        List<ParkingViewer> usersToNotify = getUsersToNotifyForFullParking(parkingId);
        logger.info("Found {} users to notify for parking {}", usersToNotify.size(), parkingId);
        
        if (usersToNotify.isEmpty()) {
            logger.info("No users to notify for parking {}", parkingId);
            return;
        }
        
        // Get parking name
        Optional<Parking> parkingOpt = parkingRepository.findById(parkingId);
        if (parkingOpt.isEmpty()) {
            logger.error("Parking with ID {} not found", parkingId);
            return;
        }
        
        String parkingName = parkingOpt.get().getName();
        
        // Extract user IDs
        List<Long> userIds = usersToNotify.stream()
                .map(ParkingViewer::getUserId)
                .collect(Collectors.toList());
        
        // Create notification DTO
        ParkingFullNotificationDTO notification = new ParkingFullNotificationDTO(
                parkingId, parkingName, userIds);
        
        // Send to RabbitMQ
        logger.info("Sending parking full notification for {} to {} users", parkingName, userIds.size());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.PARKING_FULL_ROUTING_KEY,
                    notification);
            logger.info("Successfully sent notification to RabbitMQ");
        } catch (Exception e) {
            logger.error("Failed to send notification to RabbitMQ: {}", e.getMessage(), e);
        }
        
        // Mark users as notified
        usersToNotify.forEach(viewer -> {
            logger.info("Marking user {} as notified for parking {}", viewer.getUserId(), parkingId);
            markUserAsNotified(viewer.getId());
        });
    }
    
    // Duplicate methods from ParkingViewerService to avoid circular dependency
    private boolean isParkingFull(Long parkingId) {
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
    
    private List<ParkingViewer> getUsersToNotifyForFullParking(Long parkingId) {
        LocalDateTime now = LocalDateTime.now();
        return parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(parkingId, now);
    }
    
    private void markUserAsNotified(Long viewerId) {
        parkingViewerRepository.findById(viewerId).ifPresent(viewer -> {
            viewer.setNotificationSent(true);
            parkingViewerRepository.save(viewer);
        });
    }
}
