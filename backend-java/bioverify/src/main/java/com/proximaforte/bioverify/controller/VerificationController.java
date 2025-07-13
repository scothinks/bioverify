package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.VerificationRequest;
import com.proximaforte.bioverify.dto.VerificationResponse;
import com.proximaforte.bioverify.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for the identity verification flow.
 */
@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    @Autowired
    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Endpoint for a user to submit their SSID and NIN for verification.
     * This endpoint is protected and requires an authenticated user.
     *
     * @param request The request body containing the SSID and NIN.
     * @param authentication The authentication object injected by Spring Security,
     * containing the details of the currently logged-in user.
     * @return A ResponseEntity containing the result of the verification check.
     */
    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verify(
            @RequestBody VerificationRequest request,
            Authentication authentication) {

        // 1. Get the currently authenticated user from the security context.
        User currentUser = (User) authentication.getPrincipal();

        // 2. Extract the tenantId from the user's details.
        // This ensures users can only verify themselves within their own tenant.
        if (currentUser.getTenant() == null) {
            return ResponseEntity.badRequest().body(
                new VerificationResponse(false, "User is not associated with a tenant.", null)
            );
        }
        UUID tenantId = currentUser.getTenant().getId();

        // 3. Call the service to perform the verification logic.
        VerificationResponse response = verificationService.verifyIdentity(request, tenantId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            // Return a 400 Bad Request status if verification fails, along with the message.
            return ResponseEntity.badRequest().body(response);
        }
    }
}
