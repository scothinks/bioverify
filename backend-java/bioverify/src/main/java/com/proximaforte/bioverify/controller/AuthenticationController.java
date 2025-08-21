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
    private final RefreshTokenService refreshTokenService;

    /**
     * Handles user login and returns access and refresh tokens.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    /**
     * Provides a new access token in exchange for a valid refresh token.
     */
    @PostMapping("/refreshtoken")
    public ResponseEntity<TokenRefreshResponse> refreshtoken(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(service.refreshToken(request));
    }

    /**
     * Handles user logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestBody TokenRefreshRequest request) {
        refreshTokenService.findByToken(request.getRefreshToken()).ifPresent(refreshTokenService::verifyExpiration);
        return ResponseEntity.ok(new MessageResponse("Logout successful."));
    }

    /**
     * Handles manual account creation by an administrator.
     */
    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        service.createAccount(request);
        return ResponseEntity.ok(Map.of("message", "Account created successfully. You can now log in."));
    }

    /**
     * NEW: Endpoint to handle account activation via the link sent to the user's email.
     */
    @PostMapping("/activate-account")
    public ResponseEntity<MessageResponse> activateAccount(@RequestBody AccountActivationRequest request) {
        service.activateAccount(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new MessageResponse("Account activated successfully. You can now log in."));
    }

    /**
     * NEW: Endpoint to resend an account activation link.
     */
    @PostMapping("/resend-activation")
    public ResponseEntity<MessageResponse> resendActivation(@RequestBody ResendActivationRequest request) {
        service.resendActivationLink(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("A new activation link has been sent to your email address."));
    }
}