package com.mental_health_app.mental_health.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * Currently allows unrestricted access to public pages, static assets
 * and the registration/login API endpoints. CSRF is disabled for the
 * REST API to allow JSON POST requests from the frontend JS.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)  // for H2 console
            )
            .authorizeHttpRequests(auth -> auth
                // Public pages & assets
                .requestMatchers("/", "/auth", "/auth/**").permitAll()
                .requestMatchers("/css/**", "/images/**").permitAll()
                // Public API endpoints (register / login)
                .requestMatchers("/api/patients/register", "/api/patients/login").permitAll()
                .requestMatchers("/api/therapists/register", "/api/therapists/login").permitAll()
                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Actuator health
                .requestMatchers("/actuator/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
