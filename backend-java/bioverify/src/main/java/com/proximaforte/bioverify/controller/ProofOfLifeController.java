package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.service.ProofOfLifeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller handling Proof of Life (PoL) operations.
 * 
 * Proof of Life is a critical workflow where agents verify employee identity
 * through biometric capture and document validation. This process includes:
 * - Live photo capture
 * - Document upload (Letter of Employment, Work ID)
 * - Automated validation and flagging
 * - Account creation for verified employees
 * 
 * Access is restricted to users with AGENT authority.
 */
@RestController
@RequestMapping("/api/v1/pol")
public class ProofOfLifeController {

    private final ProofOfLifeService proofOfLifeService;

    public ProofOfLifeController(ProofOfLifeService proofOfLifeService) {
        this.proofOfLifeService = proofOfLifeService;
    }

    /**
     * Completes the Proof of Life process for a specific employee record.
     * 
     * This endpoint:
     * 1. Accepts employee photo and supporting documents
     * 2. Validates document content using OCR and automated checks
     * 3. Creates user account if validation passes
     * 4. Flags invalid documents for manual review
     * 5. Sends email notification to the employee
     * 
     * @param recordId UUID of the master list record being processed
     * @param email Employee's email address for account creation
     * @param photo Live photo captured during PoL session
     * @param letterOfEmployment PDF document proving employment status
     * @param workId PDF document containing employee work identification
     * @return 200 OK if successful, 400 Bad Request if documents fail validation
     */
    @PostMapping("/{recordId}/complete")
    @PreAuthorize("hasAuthority('AGENT')")
    public ResponseEntity<?> completeProofOfLife(
            @PathVariable UUID recordId,
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("letterOfEmployment") MultipartFile letterOfEmployment,
            @RequestParam("workId") MultipartFile workId) {
        
        // Process the PoL submission and get validation result
        boolean wasSuccessful = proofOfLifeService.completePoL(recordId, email, photo, letterOfEmployment, workId);

        // Handle validation failure - documents flagged for review
        if (!wasSuccessful) {
            String message = "One or more documents failed automated content validation and has been flagged for administrative review.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
        }

        // Success - account created and email sent
        return ResponseEntity.ok().build();
    }
}