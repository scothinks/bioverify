package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.PayrollExportLog;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.*;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.PayrollExportLogRepository;
import com.proximaforte.bioverify.service.*;
import com.proximaforte.bioverify.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Slf4j
public class MasterListRecordController {

    private final MasterListUploadService uploadService;
    private final VerificationService verificationService;
    private final MasterListRecordRepository recordRepository;
    private final BulkVerificationService bulkVerificationService;
    private final MasterListRecordService recordService;
    private final ExportService exportService;
    private final PayrollExportLogRepository payrollExportLogRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/queue/pending-approval")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<List<MasterListRecordDto>> getPendingApprovalQueue(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getPendingApprovalQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    @GetMapping("/queue/mismatched")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<List<MasterListRecordDto>> getMismatchedQueue(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getMismatchedQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    // --- NEW ENDPOINT FOR "NOT IN SOT" RECORDS ---
    @GetMapping("/flagged/not-in-sot")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<MasterListRecordDto>> getFlaggedNotInSot(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getFlaggedNotInSotQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    @PutMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<MasterListRecordDto> updateRecord(
            @PathVariable UUID recordId,
            @RequestBody UpdateRecordRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        MasterListRecord updatedRecord = recordService.updateRecordData(recordId, request, currentUser);
        return ResponseEntity.ok(new MasterListRecordDto(updatedRecord));
    }

    @PostMapping("/{recordId}/validate")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<MasterListRecordDto> validateRecord(
            @PathVariable UUID recordId,
            @RequestBody ValidateRecordRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        MasterListRecord validatedRecord = recordService.performValidation(recordId, request, currentUser);
        return ResponseEntity.ok(new MasterListRecordDto(validatedRecord));
    }
    
    @PostMapping("/{recordId}/resolve-mismatch")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<MasterListRecordDto> resolveMismatch(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal User currentUser) {
        MasterListRecord resolvedRecord = recordService.resolveMismatchWithSotData(recordId, currentUser);
        return ResponseEntity.ok(new MasterListRecordDto(resolvedRecord));
    }

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
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
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

    // --- PAYROLL EXPORT ENDPOINTS ---

    @PostMapping("/export")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> initiateExport(@AuthenticationPrincipal User currentUser) {
        exportService.generateExport(currentUser);
        return ResponseEntity.accepted().body(Map.of("message", "Payroll export process has been initiated."));
    }

    @GetMapping("/export-logs")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<PayrollExportLogDto>> getExportHistory(@AuthenticationPrincipal User currentUser) {
        List<PayrollExportLog> logs = payrollExportLogRepository.findByTenantIdOrderByExportTimestampDesc(currentUser.getTenant().getId());
        List<PayrollExportLogDto> dtos = logs.stream().map(PayrollExportLogDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/export-logs/{logId}/download")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ByteArrayResource> downloadExportedFile(@PathVariable UUID logId, @AuthenticationPrincipal User currentUser) {
        PayrollExportLog exportLog = payrollExportLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Export log not found"));

        if (!exportLog.getTenant().getId().equals(currentUser.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            byte[] data = fileStorageService.load(exportLog.getFileUrl());
            ByteArrayResource resource = new ByteArrayResource(data);
            
            String filename = "payroll-export-" + exportLog.getId() + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(data.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Error loading file for export log {}: {}", logId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}