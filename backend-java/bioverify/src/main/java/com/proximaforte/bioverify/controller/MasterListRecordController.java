package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.*;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.service.BulkVerificationService;
import com.proximaforte.bioverify.service.MasterListRecordService; 
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
    private final BulkVerificationService bulkVerificationService;
    private final MasterListRecordService recordService; 

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MasterListRecordDto>> getTenantRecords(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordRepository.findAllByTenantId(currentUser.getTenant().getId());
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FOCAL_OFFICER')")
    public ResponseEntity<UploadSummaryDto> uploadMasterList(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) throws Exception {
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

    @PostMapping("/bulk-verify")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> bulkVerify(@AuthenticationPrincipal User currentUser) {
        bulkVerificationService.startBulkVerification(currentUser);
        return ResponseEntity.accepted().body(Map.of("message", "Bulk verification process for all pending records has been initiated."));
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notifyForReverification(@RequestBody NotificationRequestDto request) {
        return ResponseEntity.ok(Map.of("message", "Notification process initiated for " + request.getRecordIds().size() + " users."));
    }
    
    // --- NEW METHOD TO FIX DATA ---
    @PatchMapping("/{recordId}/psn")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> updateRecordPsn(
            @PathVariable UUID recordId,
            @RequestBody Map<String, String> requestBody) {
        String newPsn = requestBody.get("psn");
        if (newPsn == null || newPsn.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must contain a 'psn' field."));
        }
        recordService.updatePsnForRecord(recordId, newPsn);
        return ResponseEntity.ok(Map.of("message", "PSN for record " + recordId + " updated successfully."));
    }
}