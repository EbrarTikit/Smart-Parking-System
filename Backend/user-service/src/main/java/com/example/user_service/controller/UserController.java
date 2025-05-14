package com.example.user_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.NotificationPreferencesDto;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
    
    // Notification preferences endpoints
    @GetMapping("/{id}/notification-preferences")
    public ResponseEntity<NotificationPreferencesDto> getNotificationPreferences(@PathVariable Long id) {
        NotificationPreferencesDto preferences = userService.getNotificationPreferences(id);
        return ResponseEntity.ok(preferences);
    }
    
    @PutMapping("/{id}/notification-preferences/toggle")
    public ResponseEntity<NotificationPreferencesDto> toggleNotificationPreferences(@PathVariable Long id) {
        NotificationPreferencesDto updatedPreferences = userService.toggleNotificationPreferences(id);
        return ResponseEntity.ok(updatedPreferences);
    }
}
