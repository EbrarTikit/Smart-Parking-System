package com.example.user_service.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.user_service.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional <User> findByUsername(String username);
    // Özel sorgular buraya eklenebilir

    
}

