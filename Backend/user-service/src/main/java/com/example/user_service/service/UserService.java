package com.example.user_service.service;

import com.example.user_service.repository.UserRepository;
import com.example.user_service.model.User;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;




@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

  


    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }
    

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User updateUser(Long userId, User updatedUser) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmail(updatedUser.getEmail());
        user.setUsername(updatedUser.getUsername());
        // Diğer alanlar olursa güncellenebilir
        return userRepository.save(user);
    }
    
// son giriş zamanını güncellemek istersek metod yazılabilir ???
    // Diğer metodlar
}


