package com.example.user_service.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("🔍 JwtAuthenticationFilter ÇALIŞIYOR: " + request.getRequestURI());

        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("📢 Authorization Header: " + authorizationHeader);


        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            System.out.println("✅ User Found: " + userDetails.getUsername());


            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("🔒 Authentication SUCCESS");

            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("⚠ SecurityContextHolder içinde Authentication YOK! Ekleniyor...");
            } else {
                System.out.println("✅ SecurityContextHolder içinde Authentication VAR!");
            }
            System.out.println("🚀 SecurityContextHolder.getContext().getAuthentication(): " + SecurityContextHolder.getContext().getAuthentication());

            
        }
        filterChain.doFilter(request, response);
    }

}
