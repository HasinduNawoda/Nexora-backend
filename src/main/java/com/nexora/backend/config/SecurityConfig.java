package com.nexora.backend.config;

import com.nexora.backend.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TEMPORARY DIAGNOSTIC: writes the real reason a request was rejected
    // (captured by JwtFilter as the "auth_error" request attribute) into the
    // response body, instead of Spring Security's default blank 401/403.
    // Safe to revert once the root cause is confirmed.
    private void writeAuthError(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 int status) throws java.io.IOException {
        Object reason = request.getAttribute("auth_error");
        Object filterRan = request.getAttribute("jwt_filter_ran");
        Object debug = request.getAttribute("jwt_debug");
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        response.setStatus(status);
        response.setContentType("application/json");

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"status\":").append(status);
        sb.append(",\"auth_error\":\"").append(reason != null ? reason.toString().replace("\"", "'") : "null").append("\"");
        sb.append(",\"jwt_filter_ran\":\"").append(filterRan).append("\"");
        sb.append(",\"jwt_debug\":\"").append(debug).append("\"");
        sb.append(",\"security_context_auth\":\"").append(auth != null ? auth.getClass().getSimpleName() + "[" + auth.getName() + ",authenticated=" + auth.isAuthenticated() + "]" : "null").append("\"");
        sb.append(",\"request_uri\":\"").append(request.getRequestURI()).append("\"");
        sb.append(",\"method\":\"").append(request.getMethod()).append("\"");
        sb.append("}");

        response.getWriter().write(sb.toString());
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeAuthError(request, response, 401);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeAuthError(request, response, 403);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/gmail/callback").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}