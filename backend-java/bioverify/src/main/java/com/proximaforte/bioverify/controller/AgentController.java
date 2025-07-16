package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.AgentLookupRequest;
import com.proximaforte.bioverify.dto.ConfirmationRequestDto;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.dto.VerificationRequest;
import com.proximaforte.bioverify.dto.VerificationResponse;
import com.proximaforte.bioverify.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map; // <-- IMPORT ADDED
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
@PreAuthorize("hasAuthority('ENUMERATOR')")
public class AgentController {

    private final VerificationService verificationService;

    public AgentController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/lookup")
    public ResponseEntity<?> lookupRecord(@RequestBody AgentLookupRequest request, Authentication authentication) {
        // ... (lookup logic remains the same)
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyOnBehalfOf(
            @RequestBody VerificationRequest request,
            Authentication authentication) {
        // ... (verify logic remains the same)
    }

    // --- NEW ENDPOINT for Liveness Check ---
    @PostMapping("/liveness-check")
    public ResponseEntity<?> performLivenessCheck(@RequestBody ConfirmationRequestDto request) {
        try {
            // We reuse the ConfirmationRequestDto since it just needs a recordId
            verificationService.performLivenessCheck(request.getRecordId());
            return ResponseEntity.ok(Map.of("message", "Proof of Life date updated successfully."));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }
}