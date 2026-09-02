package com.mental_health_app.mental_health.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * WHAT THIS CLASS DOES:
 * ─────────────────────
 * After a user logs in successfully, Spring Security calls this class
 * to decide WHERE to redirect them.
 *
 *   - If the user is a THERAPIST → send them to /therapist/dashboard
 *   - If the user is a PATIENT   → send them to /dashboard
 *
 * Without this, all users would go to the same page after login.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Get the user's role and redirect accordingly
        // authentication.getAuthorities() returns a list of roles like [ROLE_PATIENT]

        String redirectUrl = "/dashboard";  // default: patient dashboard

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_THERAPIST")) {
                redirectUrl = "/therapist/dashboard";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
