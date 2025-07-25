package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.dto.InitiateVerificationResponseDto;
import com.proximaforte.bioverify.dto.PsnChallengeRequestDto;
import com.proximaforte.bioverify.dto.VerifyIdentityRequest;
import com.proximaforte.bioverify.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);
    private final VerificationService verificationService;

    @PostMapping("/initiate")
    public ResponseEntity<InitiateVerificationResponseDto> initiate(
            @RequestBody VerifyIdentityRequest request) {
        
        logger.info("Received public self-service initiation request for SSID: {}", request.getSsid());
        InitiateVerificationResponseDto response = verificationService.initiateSelfVerification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/challenge")
    public ResponseEntity<?> challenge(@RequestBody PsnChallengeRequestDto challengeRequest) {
        
        logger.info("Received public PSN challenge for recordId: {}", challengeRequest.getRecordId());
        boolean success = verificationService.resolvePsnChallenge(challengeRequest);

        if (success) {
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Challenge passed."));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "The provided PSN is incorrect."));
        }
    }
}