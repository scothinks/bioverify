package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.service.ProofOfLifeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pol")
public class ProofOfLifeController {

    private final ProofOfLifeService proofOfLifeService;

    public ProofOfLifeController(ProofOfLifeService proofOfLifeService) {
        this.proofOfLifeService = proofOfLifeService;
    }

    @PostMapping("/{recordId}/complete")
    @PreAuthorize("hasAuthority('AGENT')")
    public ResponseEntity<?> completeProofOfLife(
            @PathVariable UUID recordId,
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("letterOfEmployment") MultipartFile letterOfEmployment,
            @RequestParam("workId") MultipartFile workId) {
        
        boolean wasSuccessful = proofOfLifeService.completePoL(recordId, email, photo, letterOfEmployment, workId);

        if (!wasSuccessful) {
            String message = "One or more documents failed automated content validation and has been flagged for administrative review.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
        }

        return ResponseEntity.ok().build();
    }
}