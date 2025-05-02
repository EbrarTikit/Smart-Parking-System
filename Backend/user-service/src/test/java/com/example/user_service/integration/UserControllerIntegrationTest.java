package com.example.user_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.dto.JwtResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Test öncesinde veritabanını temizle
        userRepository.deleteAll();

        // Test kullanıcısı oluştur ve kaydet
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);

        // Gerçek login ile JWT token al
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                JwtResponse.class);
        
        jwtToken = response.getToken();
    }

    @Test
    void getAllUsers_ShouldReturnUsers() throws Exception {
        // İkinci bir kullanıcı ekle
        User user2 = new User();
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPassword(passwordEncoder.encode("password456"));
        userRepository.save(user2);

        // Tüm kullanıcıları getir
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Veritabanında olmayan bir ID kullan
        Long nonExistingId = 999999L;
        
        // Önce ID'nin veritabanında olmadığını doğrula
        assertFalse(userRepository.existsById(nonExistingId));
        
        mockMvc.perform(get("/api/users/{id}", nonExistingId)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_ShouldCreateUser() throws Exception {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("newpassword");

        MvcResult result = mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Response JSON'dan kullanıcı bilgilerini al
        User createdUser = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                User.class);
        
        assertEquals("newuser", createdUser.getUsername());
        assertEquals("new@example.com", createdUser.getEmail());

        // Kullanıcının veritabanında olduğunu doğrula
        assertTrue(userRepository.findByUsername("newuser").isPresent());
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Kullanıcının veritabanından silindiğini doğrula
        assertFalse(userRepository.existsById(testUser.getId()));
    }
}