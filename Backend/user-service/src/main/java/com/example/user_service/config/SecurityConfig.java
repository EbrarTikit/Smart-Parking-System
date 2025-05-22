package com.example.user_service.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.user_service.security.JwtAuthenticationFilter;
import com.example.user_service.security.JwtUtil;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setUserDetailsService(userDetailsService);
        filter.setJwtUtil(jwtUtil);
        return filter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/api/users/*/favorites/**").permitAll()  // Favori işlemleri için public erişim
                    // Açık erişim vermek istediğiniz endpointler
                    .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/users/**").permitAll()
                    .requestMatchers("/api/users/*/notification-preferences").permitAll()
                    .requestMatchers("/api/users/*/notification-preferences/toggle").permitAll()
                    .requestMatchers("/api/users/*/fcm/**").permitAll()
                    .anyRequest().authenticated()
            )
            // JWT filtresi eklemek yerine öncelikle genel endpoint erişimine izin verin
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {
        return new org.springframework.security.authentication.ProviderManager(
                List.of(new org.springframework.security.authentication.dao.DaoAuthenticationProvider() {
                    {
                        setUserDetailsService(userDetailsService);
                        setPasswordEncoder(passwordEncoder);
                    }
                }));
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedHandler() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // İzin verilen kaynakları belirtin:
        // - http://localhost:3000 (Lokal staff panel veya eski admin panel)
        // - http://localhost:3002 (Docker'daki admin panel, host'tan erişim)
        // - http://localhost:80 (Eğer nginx 80'de çalışırsa)
        // - http://admin-panel (Docker iç ağı, servis adı)
        // - Potansiyel Docker iç IP adresleri veya wildcard (test için)
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3002", "http://localhost:80", "http://admin-panel")); // Buraya "http://admin-panel" ekledik
        // Opsiyonel olarak, test ortamında her şeye izin vermek için:
        // configuration.setAllowedOrigins(List.of("*")); // DİKKAT: Üretimde kullanılmamalı!

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Kimlik bilgileri (örneğin çerezler) ile istek yapılmasına izin ver

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Tüm yollar için uygula
        return source;
    }
}
