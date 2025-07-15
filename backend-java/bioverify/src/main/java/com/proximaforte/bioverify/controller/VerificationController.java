// FILE: src/main/java/com/proximaforte/bioverify/controller/VerificationController.java

package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.ConfirmationRequestDto;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    @Autowired
    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verify(
            @RequestBody VerificationRequest request,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getTenant() == null) {
            return ResponseEntity.badRequest().body(
                new VerificationResponse(false, "User is not associated with a tenant.", null)
            );
        }
        UUID tenantId = currentUser.getTenant().getId();

        VerificationResponse response = verificationService.verifyIdentity(request, tenantId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmationRequestDto request) {
        try {
            verificationService.confirmVerification(request.getRecordId());
            return ResponseEntity.ok(Map.of("message", "Verification confirmed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}