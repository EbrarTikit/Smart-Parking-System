package com.example.user_service.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.exception.UsernameAlreadyExistsException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.UserService;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Test öncesinde veritabanını temizle
        userRepository.deleteAll();

        // Test kullanıcısı oluştur
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    @Test
    void findAllUsers_ShouldReturnAllUsers() {
        // İki kullanıcı ekle
        userService.registerUser(testUser);
        
        User user2 = new User();
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPassword("password456");
        userService.registerUser(user2);

        // Tüm kullanıcıları getir
        List<User> users = userService.findAllUsers();
        
        assertEquals(2, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        assertEquals("testuser2", users.get(1).getUsername());
    }

    @Test
    void findUserById_ShouldReturnUser() {
        User savedUser = userService.registerUser(testUser);
        
        Optional<User> foundUser = userService.findUserById(savedUser.getId());
        
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("test@example.com", foundUser.get().getEmail());
    }

    @Test
    void findUserById_WithInvalidId_ShouldReturnEmpty() {
        Optional<User> foundUser = userService.findUserById(999L);
        
        assertFalse(foundUser.isPresent());
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        userService.registerUser(testUser);
        
        User foundUser = userService.findByUsername("testuser");
        
        assertNotNull(foundUser);
        assertEquals("testuser", foundUser.getUsername());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void findByUsername_WithInvalidUsername_ShouldThrowException() {
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsername("nonexistentuser");
        });
    }

    @Test
    void registerUser_ShouldRegisterUser() {
        User savedUser = userService.registerUser(testUser);
        
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        
        // Şifrenin hash'lendiğini doğrula
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
    }

    @Test
    void registerUser_WithExistingUsername_ShouldThrowException() {
        userService.registerUser(testUser);
        
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser");
        duplicateUser.setEmail("another@example.com");
        duplicateUser.setPassword("anotherpassword");
        
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.registerUser(duplicateUser);
        });
    }

    @Test
    void deleteUser_ShouldDeleteUser() {
        User savedUser = userService.registerUser(testUser);
        
        userService.deleteUser(savedUser.getId());
        
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void existsByUsername_WithExistingUsername_ShouldReturnTrue() {
        userService.registerUser(testUser);
        
        boolean exists = userService.existsByUsername("testuser");
        
        assertTrue(exists);
    }

    @Test
    void existsByUsername_WithNonExistingUsername_ShouldReturnFalse() {
        boolean exists = userService.existsByUsername("nonexistentuser");
        
        assertFalse(exists);
    }
}