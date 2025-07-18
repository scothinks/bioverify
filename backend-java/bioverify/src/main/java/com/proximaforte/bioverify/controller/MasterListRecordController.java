package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.*;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.service.MasterListUploadService;
import com.proximaforte.bioverify.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class MasterListRecordController {

    private final MasterListUploadService uploadService;
    private final VerificationService verificationService;
    private final MasterListRecordRepository recordRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MasterListRecordDto>> getTenantRecords(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordRepository.findAllByTenantId(currentUser.getTenant().getId());
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new) // Uses the new convenience constructor
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FOCAL_OFFICER')")
    public ResponseEntity<UploadSummaryDto> uploadMasterList(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) throws Exception {
        // CORRECTED: Pass the tenant's ID (UUID) instead of the whole Tenant object.
        UploadSummaryDto summary = uploadService.processUpload(file, currentUser.getTenant().getId());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{recordId}/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VerificationResultDto> verifyRecord(
            @PathVariable UUID recordId,
            @RequestBody VerifyIdentityRequest request,
            @AuthenticationPrincipal User currentUser) {
        VerificationResultDto result = verificationService.initiateVerification(
                currentUser.getTenant().getId(), recordId, request.getSsid(), request.getNin()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notifyForReverification(@RequestBody NotificationRequestDto request) {
        return ResponseEntity.ok(Map.of("message", "Notification process initiated for " + request.getRecordIds().size() + " users."));
    }
}