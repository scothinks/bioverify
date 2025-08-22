package com.proximaforte.bioverify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the BioVerify application.
 * 
 * This configuration establishes a comprehensive security framework including:
 * - JWT-based stateless authentication without sessions
 * - Role-based authorization with method-level security
 * - CORS configuration for cross-origin requests
 * - Security headers for protection against common attacks
 * - Public endpoints for authentication and file access
 * 
 * The security model supports multi-tenant isolation and role-based access control
 * across 5 user roles: GLOBAL_SUPER_ADMIN, TENANT_ADMIN, REVIEWER, AGENT, SELF_SERVICE_USER.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity  // Enables @PreAuthorize annotations on methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // Configurable CORS origins for different deployment environments
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:4200}")
    private List<String> allowedOrigins;

    // CORS preflight cache duration
    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;
    
    /**
     * Removes the default "ROLE_" prefix from Spring Security authorities.
     * This allows using clean role names like "TENANT_ADMIN" instead of "ROLE_TENANT_ADMIN".
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

    /**
     * Configures the main security filter chain for HTTP requests.
     * 
     * This method sets up:
     * 1. CORS configuration for cross-origin requests
     * 2. Public endpoints that bypass authentication
     * 3. Stateless session management (no HTTP sessions)
     * 4. JWT authentication filter integration
     * 5. Comprehensive security headers for attack prevention
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // === PUBLIC ENDPOINTS (No authentication required) ===
                        .requestMatchers("/api/v1/auth/**").permitAll()          // Authentication endpoints
                        .requestMatchers("/api/v1/verification/**").permitAll()  // Public verification endpoints
                        .requestMatchers("/files/**").permitAll()               // File access for documents/images
                        .requestMatchers("/api/v1/health", "/actuator/health").permitAll() // Health checks
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // API documentation
                        .requestMatchers("/error", "/error/**").permitAll()     // Error pages
                        .requestMatchers("/favicon.ico", "/robots.txt").permitAll() // Static resources
                        
                        // === PROTECTED ENDPOINTS (Authentication required) ===
                        .anyRequest().authenticated()  // All other endpoints require valid JWT
                )
                // Configure stateless sessions (no server-side session storage)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Register custom authentication provider and JWT filter
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                
                // === SECURITY HEADERS CONFIGURATION ===
                .headers(headers -> headers
                    // Prevent page embedding in frames (clickjacking protection)
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    // Prevent MIME type sniffing
                    .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::and)
                    // HTTP Strict Transport Security (force HTTPS)
                    .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)  // Apply to all subdomains
                        .preload(true)            // Include in browser preload lists
                        .maxAgeInSeconds(31536000) // 1 year validity
                    )
                    // Referrer policy for privacy protection
                    .referrerPolicy(policy -> policy
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                )
                .build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings for the application.
     * 
     * This configuration allows the Angular frontend to communicate with the Spring Boot backend
     * across different origins (domains/ports). Essential for development and production deployments
     * where frontend and backend may be served from different domains.
     * 
     * Uses externalized configuration properties for flexibility across different environments
     * (development, staging, production).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins from application properties
        // Default: localhost:3000, localhost:4200, localhost:8080 for development
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Allow all common HTTP methods for REST API operations
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers that the client is allowed to send
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",      // JWT tokens
            "Content-Type",       // JSON/form data
            "X-Requested-With"    // AJAX request identification
        ));
        
        // Headers that the client can access from the response
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",      // Updated JWT tokens
            "X-Total-Count"       // Pagination information
        ));
        
        // Allow credentials (cookies, authorization headers) in CORS requests
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests to improve performance (default 1 hour)
        configuration.setMaxAge(Duration.ofSeconds(corsMaxAge));
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}