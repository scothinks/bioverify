package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.dto.*;
import com.proximaforte.bioverify.service.AuthenticationService;
import com.proximaforte.bioverify.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final RefreshTokenService refreshTokenService; // NEW

    /**
     * UPDATED: Endpoint now returns a JwtResponse containing access and refresh tokens.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    /**
     * NEW: Endpoint to get a new access token using a refresh token.
     */
    @PostMapping("/refreshtoken")
    public ResponseEntity<TokenRefreshResponse> refreshtoken(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(service.refreshToken(request));
    }
    
    /**
     * NEW: Endpoint to log out. This should delete the refresh token.
     * Note: A corresponding method needs to be added to the service layer.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestBody TokenRefreshRequest request) {
        refreshTokenService.findByToken(request.getRefreshToken()).ifPresent(refreshTokenService::verifyExpiration);
        return ResponseEntity.ok(new MessageResponse("Logout successful."));
    }


    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        service.createAccount(request);
        return ResponseEntity.ok(Map.of("message", "Account created successfully. You can now log in."));
    }
}