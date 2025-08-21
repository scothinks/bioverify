package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.service.LivenessCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/liveness")
public class LivenessCheckController {

    private final LivenessCheckService livenessCheckService;

    public LivenessCheckController(LivenessCheckService livenessCheckService) {
        this.livenessCheckService = livenessCheckService;
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('SELF_SERVICE_USER')")
    public ResponseEntity<?> submitLivenessCheck(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("livenessVideo") MultipartFile livenessVideo) {
        
        livenessCheckService.processLivenessCheck(currentUser, livenessVideo);
        return ResponseEntity.ok(Map.of("success", true, "message", "Liveness check submitted successfully."));
    }
}