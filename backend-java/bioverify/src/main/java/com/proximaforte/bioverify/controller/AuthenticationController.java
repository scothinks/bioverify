package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.dto.AuthResponse;
import com.proximaforte.bioverify.dto.AuthenticationRequest;
import com.proximaforte.bioverify.dto.CreateAccountRequest;
import com.proximaforte.bioverify.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        service.createAccount(request);
        return ResponseEntity.ok("Account created successfully. You can now log in.");
    }
}