package com.nexora.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        request.setAttribute("jwt_filter_ran", "true");

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            request.setAttribute("auth_error", "No Authorization header sent to the backend");
            request.setAttribute("jwt_debug", "no_auth_header");
        } else {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    request.setAttribute("jwt_debug", "auth_set_for_" + username);
                } else {
                    request.setAttribute("auth_error", "Token failed validation (isTokenValid returned false)");
                    request.setAttribute("jwt_debug", "token_invalid");
                }
            } catch (Exception e) {
                request.setAttribute("auth_error", "Token parsing threw: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                request.setAttribute("jwt_debug", "exception_" + e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }
}