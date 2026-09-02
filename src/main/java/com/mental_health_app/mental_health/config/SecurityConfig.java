package com.mental_health_app.mental_health.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SPRING SECURITY CONFIGURATION
 * ─────────────────────────────
 * This class controls:
 *   1. Which pages are PUBLIC (anyone can access)
 *   2. Which pages need LOGIN (only logged-in users)
 *   3. Which pages are for PATIENTS only or THERAPISTS only
 *   4. How login and logout work
 *
 * Spring Security automatically uses our CustomUserDetailsService
 * (because it's a @Service that implements UserDetailsService)
 * and our PasswordEncoder bean (defined below) — no extra wiring needed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    /**
     * BCrypt password encoder — used for hashing passwords.
     * Spring Security automatically picks this up.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The main security configuration.
     * Think of this as a set of RULES for your application.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ─── Allow H2 console to load in an iframe ───
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )

            // ─── RULE 1: Which URLs are public vs protected ───
            .authorizeHttpRequests(auth -> auth

                // PUBLIC — anyone can access these (no login needed)
                .requestMatchers("/", "/auth", "/auth/**").permitAll()
                .requestMatchers("/css/**", "/images/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // PATIENTS ONLY — must be logged in as a patient
                .requestMatchers("/dashboard/**").hasRole("PATIENT")
                .requestMatchers("/appointments/book").hasRole("PATIENT")
                .requestMatchers("/assessments/**").hasRole("PATIENT")
                .requestMatchers("/chat/**").hasRole("PATIENT")

                // THERAPISTS ONLY — must be logged in as a therapist
                .requestMatchers("/therapist/**").hasRole("THERAPIST")

                // APPOINTMENTS (common status updates) — must be logged in
                .requestMatchers("/appointments/**").authenticated()

                // EVERYTHING ELSE — must be logged in (any role)
                .anyRequest().authenticated()
            )

            // ─── RULE 2: How login works ───
            .formLogin(form -> form
                .loginPage("/auth?mode=login")       // our custom login page (not the default Spring one)
                .loginProcessingUrl("/auth/login")    // the URL where the login form POSTs to
                .usernameParameter("email")           // our form uses "email" field (not "username")
                .passwordParameter("password")        // our form uses "password" field
                .successHandler(successHandler)        // after login → redirect based on role
                .failureUrl("/auth?mode=login&error=true")  // if login fails → go back to login page
                .permitAll()
            )

            // ─── RULE 3: How logout works ───
            .logout(logout -> logout
                .logoutUrl("/logout")                 // POST to /logout to log out
                .logoutSuccessUrl("/?logout=true")    // after logout → go to home page
                .invalidateHttpSession(true)          // destroy the session
                .deleteCookies("JSESSIONID")          // delete the session cookie
                .permitAll()
            )

            // ─── CSRF protection ───
            // CSRF is a security feature that prevents fake form submissions.
            // We keep it ON everywhere, but disable it for H2 console (dev tool).
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );

        return http.build();
    }
}
