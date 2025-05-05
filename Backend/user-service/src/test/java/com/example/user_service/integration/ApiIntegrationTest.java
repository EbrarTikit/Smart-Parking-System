package com.example.user_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.dto.JwtResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.SignupRequest;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Test öncesinde veritabanını temizle
        userRepository.deleteAll();

        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void completeUserFlow_ShouldWorkCorrectly() throws Exception {
        // 1. Kullanıcı kaydı
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // 2. Kullanıcı girişi ve token alımı
        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(loginResponse, JwtResponse.class);
        String token = jwtResponse.getToken();
        Long userId = jwtResponse.getUserId();

        assertNotNull(token);
        assertNotNull(userId);

        // 3. Kullanıcı bilgilerini getir
        mockMvc.perform(get("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // 4. Tüm kullanıcıları listele
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));

        // 5. Yeni bir kullanıcı oluştur
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("newpassword");

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));

        // 6. Kullanıcıyı sil
        mockMvc.perform(delete("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Kullanıcının silindiğini doğrula
        mockMvc.perform(get("/api/users/{id}", userId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
