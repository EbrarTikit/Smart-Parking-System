package com.example.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.dto.JwtResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.SignupRequest;
import com.example.user_service.exception.InvalidCredentialsException;
import com.example.user_service.model.User;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.CustomUserDetailsService;
import com.example.user_service.service.UserService;



@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3002", "http://localhost:80" })
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(authentication.getName());

            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            return ResponseEntity.ok(new JwtResponse(jwt, user.getId()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {
        try {
            // Kullanıcı zaten var mı kontrol et
            if (userService.existsByUsername(signupRequest.getUsername())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            // Yeni kullanıcı oluştur
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(signupRequest.getPassword());

            User savedUser = userService.registerUser(user);

            return ResponseEntity.ok(new MessageResponse("User registered successfully with ID: " + savedUser.getId()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
