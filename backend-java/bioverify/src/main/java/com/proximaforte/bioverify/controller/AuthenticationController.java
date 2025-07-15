package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.dto.AuthenticationRequest;
import com.proximaforte.bioverify.dto.AuthenticationResponse;
import com.proximaforte.bioverify.dto.EmployeeRegisterRequest;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService service;

    @Autowired
    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    /**
     * Endpoint for registering a new admin user.
     * @param request The registration request containing user details.
     * @return A ResponseEntity containing the JWT for the newly registered user.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    /**
     * NEW: Endpoint for an employee on the master list to register (claim their account).
     * @param request The employee registration request.
     * @return A ResponseEntity containing the JWT for the newly registered user.
     */
    @PostMapping("/register-employee")
    public ResponseEntity<AuthenticationResponse> registerEmployee(
            @RequestBody EmployeeRegisterRequest request
    ) {
        return ResponseEntity.ok(service.registerEmployee(request));
    }

    /**
     * Endpoint for authenticating an existing user and issuing a JWT.
     * @param request The authentication request containing email and password.
     * @return A ResponseEntity containing the JWT for the logged-in user.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
}