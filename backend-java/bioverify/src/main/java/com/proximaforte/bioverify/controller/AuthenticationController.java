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

/**
 * REST Controller handling authentication and account management operations.
 * 
 * This controller provides endpoints for:
 * - User login/logout with JWT token management
 * - Token refresh mechanism for extended sessions
 * - Account creation and activation workflow
 * - Email-based account activation system
 * 
 * All endpoints return appropriate HTTP status codes and JSON responses.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates a user and returns JWT access and refresh tokens.
     * 
     * @param request Contains email and password for authentication
     * @return JwtResponse with access token, refresh token, and user details
     */
    @PostMapping("/authenticate")
    public ResponseEntity<JwtResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * 
     * @param request Contains the refresh token
     * @return TokenRefreshResponse with new access token
     */
    @PostMapping("/refreshtoken")
    public ResponseEntity<TokenRefreshResponse> refreshtoken(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(service.refreshToken(request));
    }

    /**
     * Logs out a user by invalidating their refresh token.
     * 
     * @param request Contains the refresh token to invalidate
     * @return Success message confirming logout
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestBody TokenRefreshRequest request) {
        refreshTokenService.findByToken(request.getRefreshToken()).ifPresent(refreshTokenService::verifyExpiration);
        return ResponseEntity.ok(new MessageResponse("Logout successful."));
    }

    /**
     * Creates a new user account manually (typically by administrators).
     * 
     * @param request Contains user details for account creation
     * @return Success message confirming account creation
     */
    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        service.createAccount(request);
        return ResponseEntity.ok(Map.of("message", "Account created successfully. You can now log in."));
    }

    /**
     * Activates a user account using the activation token sent via email.
     * 
     * @param request Contains activation token and password to set
     * @return Success message confirming account activation
     */
    @PostMapping("/activate-account")
    public ResponseEntity<MessageResponse> activateAccount(@RequestBody AccountActivationRequest request) {
        service.activateAccount(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new MessageResponse("Account activated successfully. You can now log in."));
    }

    /**
     * Resends an activation link to the user's email address.
     * 
     * @param request Contains the email address to send activation link to
     * @return Success message confirming email sent
     */
    @PostMapping("/resend-activation")
    public ResponseEntity<MessageResponse> resendActivation(@RequestBody ResendActivationRequest request) {
        service.resendActivationLink(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("A new activation link has been sent to your email address."));
    }
}