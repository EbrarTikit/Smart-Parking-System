package com.example.user_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.user_service.dto.NotificationPreferencesDto;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.exception.UsernameAlreadyExistsException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        // Önce kullanıcının var olup olmadığını kontrol et
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        
        // Kullanıcı varsa sil
        userRepository.deleteById(id);
    }

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists: " + user.getUsername());
        }
        
        // Şifreyi kodla
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Kullanıcıyı kaydet
        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    // Notification preferences methods
    public NotificationPreferencesDto getNotificationPreferences(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
            
        return new NotificationPreferencesDto(user.isParkingFullNotification());
    }
    
    public NotificationPreferencesDto toggleNotificationPreferences(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
            
        // Mevcut değerin tersini al
        boolean newValue = !user.isParkingFullNotification();
        
        // Debug için değerleri yazdır
        System.out.println("Önceki değer: " + user.isParkingFullNotification());
        System.out.println("Yeni değer: " + newValue);
        
        // Yeni değeri ayarla
        user.setParkingFullNotification(newValue);
        User savedUser = userRepository.save(user);
        
        // Kaydedilen değeri kontrol et
        System.out.println("Kaydedilen değer: " + savedUser.isParkingFullNotification());
        
        return new NotificationPreferencesDto(savedUser.isParkingFullNotification());
    }
}

