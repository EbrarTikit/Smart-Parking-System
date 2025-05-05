package com.example.navigation_service.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class NavigationServiceApplicationTest {
    
    @MockBean
    private RestTemplate restTemplate;
    
    @Test
    void contextLoads() {
        // Spring Boot uygulamasının context'inin başarıyla yüklenmesini test eder
    }
    
    @Test
    void mainMethod_ShouldStartApplication() {
        // Ana metodu test eder
        NavigationServiceApplication.main(new String[]{});
    }
}