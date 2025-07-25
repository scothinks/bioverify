package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.InitiateVerificationResponseDto;
import com.proximaforte.bioverify.dto.PsnChallengeRequestDto;
import com.proximaforte.bioverify.dto.VerificationResultDto;
import com.proximaforte.bioverify.dto.VerifyIdentityRequest;
import com.proximaforte.bioverify.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    private final VerificationService verificationService;

    /**
     * Endpoint for agents to initiate verification using only SSID/NIN ("walk-up" scenario).
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<InitiateVerificationResponseDto> verifyOnBehalfOf(
            @RequestBody VerifyIdentityRequest request) {

        logger.info("Received AGENT verification request for SSID: {}", request.getSsid());
        InitiateVerificationResponseDto response = verificationService.initiateSelfVerification(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for agents to resolve a PSN challenge for the "walk-up" scenario.
     */
    @PostMapping("/challenge")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> challengeOnBehalfOf(@RequestBody PsnChallengeRequestDto challengeRequest) {

        logger.info("Received AGENT PSN challenge for recordId: {}", challengeRequest.getRecordId());
        boolean success = verificationService.resolvePsnChallenge(challengeRequest);

        if (success) {
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Challenge passed."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "The provided PSN is incorrect."));
        }
    }

    /**
     * NEW: Endpoint for authenticated users (Agents or self-service users)
     * to re-verify a record when the recordId is already known.
     * This supports the User Dashboard "Re-verify" feature.
     */
    @PostMapping("/records/{recordId}/verify")
    @PreAuthorize("hasAnyRole('AGENT', 'SELF_SERVICE_USER')")
    public ResponseEntity<VerificationResultDto> verifyKnownRecord(
            @PathVariable UUID recordId,
            @RequestBody VerifyIdentityRequest request,
            @AuthenticationPrincipal User currentUser) {

        logger.info("User {} is re-verifying record {}", currentUser.getEmail(), recordId);
        VerificationResultDto result = verificationService.initiateVerification(
                currentUser.getTenant().getId(), recordId, request.getSsid(), request.getNin()
        );
        return ResponseEntity.ok(result);
    }
}