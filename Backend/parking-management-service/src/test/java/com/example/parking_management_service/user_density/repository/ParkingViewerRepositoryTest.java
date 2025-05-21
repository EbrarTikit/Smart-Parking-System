package com.example.parking_management_service.user_density.repository;

import com.example.parking_management_service.user_density.model.ParkingViewer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ParkingViewerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParkingViewerRepository parkingViewerRepository;

    @Test
    void findByParkingIdAndExpiryTimeGreaterThan_ShouldReturnOnlyActiveViewers() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        // Create active viewer
        ParkingViewer activeViewer = new ParkingViewer();
        activeViewer.setUserId(1L);
        activeViewer.setParkingId(1L);
        activeViewer.setViewingStartTime(now.minusMinutes(10));
        activeViewer.setExpiryTime(now.plusMinutes(35));
        activeViewer.setNotificationSent(false);
        entityManager.persist(activeViewer);
        
        // Create expired viewer
        ParkingViewer expiredViewer = new ParkingViewer();
        expiredViewer.setUserId(2L);
        expiredViewer.setParkingId(1L);
        expiredViewer.setViewingStartTime(now.minusMinutes(60));
        expiredViewer.setExpiryTime(now.minusMinutes(15));
        expiredViewer.setNotificationSent(false);
        entityManager.persist(expiredViewer);
        
        entityManager.flush();
        
        // Act
        List<ParkingViewer> activeViewers = parkingViewerRepository.findByParkingIdAndExpiryTimeGreaterThan(1L, now);
        
        // Assert
        assertEquals(1, activeViewers.size());
        assertEquals(activeViewer.getUserId(), activeViewers.get(0).getUserId());
    }

    @Test
    void findActiveNonNotifiedViewersByParkingId_ShouldReturnOnlyActiveNonNotifiedViewers() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        // Create active non-notified viewer
        ParkingViewer activeNonNotifiedViewer = new ParkingViewer();
        activeNonNotifiedViewer.setUserId(1L);
        activeNonNotifiedViewer.setParkingId(1L);
        activeNonNotifiedViewer.setViewingStartTime(now.minusMinutes(10));
        activeNonNotifiedViewer.setExpiryTime(now.plusMinutes(35));
        activeNonNotifiedViewer.setNotificationSent(false);
        entityManager.persist(activeNonNotifiedViewer);
        
        // Create active notified viewer
        ParkingViewer activeNotifiedViewer = new ParkingViewer();
        activeNotifiedViewer.setUserId(2L);
        activeNotifiedViewer.setParkingId(1L);
        activeNotifiedViewer.setViewingStartTime(now.minusMinutes(10));
        activeNotifiedViewer.setExpiryTime(now.plusMinutes(35));
        activeNotifiedViewer.setNotificationSent(true);
        entityManager.persist(activeNotifiedViewer);
        
        // Create expired non-notified viewer
        ParkingViewer expiredViewer = new ParkingViewer();
        expiredViewer.setUserId(3L);
        expiredViewer.setParkingId(1L);
        expiredViewer.setViewingStartTime(now.minusMinutes(60));
        expiredViewer.setExpiryTime(now.minusMinutes(15));
        expiredViewer.setNotificationSent(false);
        entityManager.persist(expiredViewer);
        
        entityManager.flush();
        
        // Act
        List<ParkingViewer> activeNonNotifiedViewers = 
            parkingViewerRepository.findActiveNonNotifiedViewersByParkingId(1L, now);
        
        // Assert
        assertEquals(1, activeNonNotifiedViewers.size());
        assertEquals(activeNonNotifiedViewer.getUserId(), activeNonNotifiedViewers.get(0).getUserId());
    }

    @Test
    void countActiveViewersByParkingId_ShouldReturnCorrectCount() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        // Create active viewers
        ParkingViewer activeViewer1 = new ParkingViewer();
        activeViewer1.setUserId(1L);
        activeViewer1.setParkingId(1L);
        activeViewer1.setViewingStartTime(now.minusMinutes(10));
        activeViewer1.setExpiryTime(now.plusMinutes(35));
        entityManager.persist(activeViewer1);
        
        ParkingViewer activeViewer2 = new ParkingViewer();
        activeViewer2.setUserId(2L);
        activeViewer2.setParkingId(1L);
        activeViewer2.setViewingStartTime(now.minusMinutes(5));
        activeViewer2.setExpiryTime(now.plusMinutes(40));
        entityManager.persist(activeViewer2);
        
        // Create expired viewer
        ParkingViewer expiredViewer = new ParkingViewer();
        expiredViewer.setUserId(3L);
        expiredViewer.setParkingId(1L);
        expiredViewer.setViewingStartTime(now.minusMinutes(60));
        expiredViewer.setExpiryTime(now.minusMinutes(15));
        entityManager.persist(expiredViewer);
        
        // Create active viewer for different parking
        ParkingViewer differentParkingViewer = new ParkingViewer();
        differentParkingViewer.setUserId(4L);
        differentParkingViewer.setParkingId(2L);
        differentParkingViewer.setViewingStartTime(now.minusMinutes(10));
        differentParkingViewer.setExpiryTime(now.plusMinutes(35));
        entityManager.persist(differentParkingViewer);
        
        entityManager.flush();
        
        // Act
        Long count = parkingViewerRepository.countActiveViewersByParkingId(1L, now);
        
        // Assert
        assertEquals(2L, count);
    }
    
    @Test
    void findByUserIdAndParkingId_ShouldReturnCorrectViewer() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ParkingViewer viewer = new ParkingViewer();
        viewer.setUserId(1L);
        viewer.setParkingId(1L);
        viewer.setViewingStartTime(now.minusMinutes(10));
        viewer.setExpiryTime(now.plusMinutes(35));
        entityManager.persist(viewer);
        
        ParkingViewer anotherViewer = new ParkingViewer();
        anotherViewer.setUserId(2L);
        anotherViewer.setParkingId(1L);
        anotherViewer.setViewingStartTime(now.minusMinutes(5));
        anotherViewer.setExpiryTime(now.plusMinutes(40));
        entityManager.persist(anotherViewer);
        
        entityManager.flush();
        
        // Act
        var result = parkingViewerRepository.findByUserIdAndParkingId(1L, 1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(viewer.getId(), result.get().getId());
    }
    
    @Test
    void findByExpiryTimeLessThan_ShouldReturnExpiredViewers() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        // Create expired viewer
        ParkingViewer expiredViewer1 = new ParkingViewer();
        expiredViewer1.setUserId(1L);
        expiredViewer1.setParkingId(1L);
        expiredViewer1.setViewingStartTime(now.minusMinutes(60));
        expiredViewer1.setExpiryTime(now.minusMinutes(15));
        entityManager.persist(expiredViewer1);
        
        // Create another expired viewer
        ParkingViewer expiredViewer2 = new ParkingViewer();
        expiredViewer2.setUserId(2L);
        expiredViewer2.setParkingId(2L);
        expiredViewer2.setViewingStartTime(now.minusMinutes(50));
        expiredViewer2.setExpiryTime(now.minusMinutes(5));
        entityManager.persist(expiredViewer2);
        
        // Create active viewer
        ParkingViewer activeViewer = new ParkingViewer();
        activeViewer.setUserId(3L);
        activeViewer.setParkingId(1L);
        activeViewer.setViewingStartTime(now.minusMinutes(10));
        activeViewer.setExpiryTime(now.plusMinutes(35));
        entityManager.persist(activeViewer);
        
        entityManager.flush();
        
        // Act
        List<ParkingViewer> expiredViewers = parkingViewerRepository.findByExpiryTimeLessThan(now);
        
        // Assert
        assertEquals(2, expiredViewers.size());
    }
}
