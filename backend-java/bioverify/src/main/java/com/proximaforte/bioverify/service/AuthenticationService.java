// FILE: src/main/java/com/proximaforte/bioverify/service/AuthenticationService.java

package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Tenant; // <-- IMPORT
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role; // <-- IMPORT
import com.proximaforte.bioverify.dto.AuthenticationRequest;
import com.proximaforte.bioverify.dto.AuthenticationResponse;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.repository.TenantRepository; // <-- IMPORT
import com.proximaforte.bioverify.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository; // <-- INJECT
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository; // <-- INJECT
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse register(RegisterRequest request) {
        var user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        // --- NEW LOGIC FOR TENANT LINKING ---
        if (request.getRole() != Role.GLOBAL_SUPER_ADMIN && request.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new IllegalStateException("Tenant not found for ID: " + request.getTenantId()));
            user.setTenant(tenant);
        }
        // ------------------------------------

        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }
}
