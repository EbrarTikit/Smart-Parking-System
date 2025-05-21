package com.example.notification_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Load the service account key JSON file
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ClassPathResource("firebase/firebase-service-account.json").getInputStream());
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        
        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}