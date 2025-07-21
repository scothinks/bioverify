package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.VerifyIdentityRequest;
import com.proximaforte.bioverify.dto.VerificationResultDto;
import com.proximaforte.bioverify.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/Agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Agent')")
public class AgentController {

    private final VerificationService verificationService;

    @PostMapping("/records/{recordId}/verify")
    public ResponseEntity<VerificationResultDto> verifyOnBehalfOf(
            @PathVariable UUID recordId,
            @RequestBody VerifyIdentityRequest request,
            @AuthenticationPrincipal User Agent) {
        
        VerificationResultDto result = verificationService.initiateVerification(
            Agent.getTenant().getId(), recordId, request.getSsid(), request.getNin()
        );
        return ResponseEntity.ok(result);
    }
}