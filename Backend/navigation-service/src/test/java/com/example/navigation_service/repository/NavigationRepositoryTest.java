package com.example.navigation_service.repository;

import com.example.navigation_service.starter.NavigationServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = NavigationServiceApplication.class)
@ActiveProfiles("test")
public class NavigationRepositoryTest {

    @Autowired
    private INavigationRepository navigationRepository;

    @Test
    void testRepositoryLoads() {
        assertNotNull(navigationRepository);
    }
    
    // Repository'de özel sorgular varsa onları da test edebilirsiniz
}