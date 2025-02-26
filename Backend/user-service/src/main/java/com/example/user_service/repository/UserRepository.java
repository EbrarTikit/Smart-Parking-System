package com.example.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.user_service.model.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // başka sorgu metodları??
    User findByEmail(String email);
    Optional<User> findByUsername(String username);
}

