package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.service.ProofOfLifeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
    public ResponseEntity<Void> completeProofOfLife(
            @PathVariable UUID recordId,
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("letterOfEmployment") MultipartFile letterOfEmployment,
            @RequestParam("workId") MultipartFile workId) {
        
        proofOfLifeService.completePoL(recordId, email, photo, letterOfEmployment, workId);
        return ResponseEntity.ok().build();
    }
}