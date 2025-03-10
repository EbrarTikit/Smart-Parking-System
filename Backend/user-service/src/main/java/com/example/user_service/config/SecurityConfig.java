package com.example.user_service.config;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.user_service.security.JwtAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

 /*    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // CSRF korumasını devre dışı bırak
            .authorizeRequests() // Yetkilendirme taleplerini başlat
                .antMatchers("/api/auth/**").permitAll() // /api/auth/** yoluna tüm isteklere izin ver
                .anyRequest().authenticated() // Diğer tüm istekler için kimlik doğrulaması gerekir
            .and()
            .httpBasic(); // HTTP Basic kullanarak kimlik doğrulama yap
        return http.build();
    }*/

   /*  @Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/test/**").permitAll()
                    .requestMatchers("/api/protected/**").authenticated() // Protected endpointler sadece yetkilendirilmiş kullanıcılara açık
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception.accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("Access Denied!");
    }))
                    .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class); // JWT doğrulama filtresi ekleniyor

           
        return http.build(); // Bu şekilde yapılandırmanızı tamamlayın
    }*/

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> 
            auth.requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/protected/**").authenticated()
                .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class) // JWT doğrulama filtresi ekleniyor
        .addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
                    throws ServletException, java.io.IOException {
                String path = request.getRequestURI();
                
                // Skip token validation for public endpoints
                if (path.startsWith("/api/auth/") || path.startsWith("/api/test/")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Continue with token validation for protected endpoints
                final String authorizationHeader = request.getHeader("Authorization");
                // Rest of your filter code...
            }
        }, JwtAuthenticationFilter.class);
    
    return http.build();
}


    @Bean
public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
    return new org.springframework.security.authentication.ProviderManager(
            List.of(new org.springframework.security.authentication.dao.DaoAuthenticationProvider() {{
                setUserDetailsService(userDetailsService);
                setPasswordEncoder(passwordEncoder);
            }})
    );
}
    
    @Bean
public AuthenticationEntryPoint unauthorizedHandler() {
    return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
