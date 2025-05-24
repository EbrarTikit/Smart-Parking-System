package com.example.notification_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccount;
        
        if (firebaseConfigPath.startsWith("classpath:")) {
            // Classpath'ten yükle
            String resourcePath = firebaseConfigPath.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(resourcePath);
            serviceAccount = resource.getInputStream();
        } else {
            // Dosya sisteminden yükle
            FileInputStream serviceAccountStream = new FileInputStream(firebaseConfigPath);
            serviceAccount = serviceAccountStream;
        }
        
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        
        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}