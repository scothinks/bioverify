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

/**
 * REST Controller for managing employee master list records throughout their lifecycle.
 * 
 * This controller handles the complete employee verification workflow including:
 * - Master list file uploads (CSV/Excel)
 * - Queue management for different review stages
 * - Record validation and approval processes
 * - Bulk verification operations against external systems
 * - Payroll export generation and download
 * - Document approval for flagged content
 * 
 * Security is role-based with different endpoints accessible to:
 * - TENANT_ADMIN: Full access to all operations
 * - REVIEWER: Access to review queues and validation
 * - AGENT: Access to PoL record lookups
 */
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Slf4j
public class MasterListRecordController {

    // Service dependencies for various record operations
    private final MasterListUploadService uploadService;
    private final MasterListRecordRepository recordRepository;
    private final BulkVerificationService bulkVerificationService;
    private final MasterListRecordService recordService;
    private final ExportService exportService;
    private final PayrollExportLogRepository payrollExportLogRepository;
    private final FileStorageService fileStorageService;

    /**
     * Finds an employee record eligible for Proof of Life processing.
     * Used by agents to lookup records before conducting PoL sessions.
     * 
     * @param request Contains SSID/NIN for record identification
     * @param currentUser The authenticated agent user
     * @return Employee record details if found and eligible
     */
    @PostMapping("/find-for-pol")
    @PreAuthorize("hasAuthority('AGENT')")
    public ResponseEntity<MasterListRecordDto> findRecordForPol(
            @RequestBody FindRecordRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        MasterListRecord record = recordService.findRecordForPol(request, currentUser);
        return ResponseEntity.ok(new MasterListRecordDto(record));
    }

    /**
     * Retrieves records awaiting review after successful verification.
     * Filtered based on reviewer's ministry/department assignments.
     * 
     * @param currentUser The authenticated reviewer or tenant admin
     * @return List of records ready for final approval
     */
    @GetMapping("/queue/awaiting-review")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<List<MasterListRecordDto>> getAwaitingReviewQueue(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getAwaitingReviewQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

    /**
     * Retrieves records with data mismatches between uploaded data and Source of Truth.
     * Requires manual review to resolve discrepancies.
     * 
     * @param currentUser The authenticated reviewer or tenant admin
     * @return List of records with data mismatches requiring resolution
     */
    @GetMapping("/queue/mismatched")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<List<MasterListRecordDto>> getMismatchedQueue(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getMismatchedQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }
    
    /**
     * Retrieves records with documents that failed automated validation.
     * These require manual review to approve or reject flagged documents.
     * 
     * @param currentUser The authenticated tenant admin
     * @return List of records with invalid documents awaiting approval
     */
    @GetMapping("/queue/invalid-documents")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<List<MasterListRecordDto>> getInvalidDocumentQueue(@AuthenticationPrincipal User currentUser) {
        List<MasterListRecord> records = recordService.getInvalidDocumentQueue(currentUser);
        List<MasterListRecordDto> recordDtos = records.stream()
                .map(MasterListRecordDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(recordDtos);
    }

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
    
    /**
     * Dedicated endpoint for approving a document that failed automated checks.
     * This triggers the final activation and user creation flow.
     */
    @PostMapping("/{recordId}/approve-flagged-document")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'REVIEWER')")
    public ResponseEntity<MasterListRecordDto> approveFlaggedDocument(
            @PathVariable UUID recordId,
            @AuthenticationPrincipal User currentUser) {
        MasterListRecord approvedRecord = recordService.approveFlaggedDocument(recordId, currentUser);
        return ResponseEntity.ok(new MasterListRecordDto(approvedRecord));
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

    // === PAYROLL EXPORT ENDPOINTS ===
    
    /**
     * Initiates asynchronous payroll export generation for validated records.
     * Creates a CSV file containing all employees eligible for payroll processing.
     * 
     * @param currentUser The authenticated tenant admin
     * @return Acceptance message indicating export process started
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> initiateExport(@AuthenticationPrincipal User currentUser) {
        exportService.generateExport(currentUser);
        return ResponseEntity.accepted().body(Map.of("message", "Payroll export process has been initiated."));
    }

    /**
     * Retrieves the export history showing all previously generated payroll files.
     * Includes metadata such as export timestamp, record count, and file URLs.
     * 
     * @param currentUser The authenticated tenant admin
     * @return List of export log entries ordered by most recent first
     */
    @GetMapping("/export-logs")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<PayrollExportLogDto>> getExportHistory(@AuthenticationPrincipal User currentUser) {
        List<PayrollExportLog> logs = payrollExportLogRepository.findByTenantIdOrderByExportTimestampDesc(currentUser.getTenant().getId());
        List<PayrollExportLogDto> dtos = logs.stream().map(PayrollExportLogDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Downloads a previously generated payroll export file.
     * Includes security checks to ensure users can only access their tenant's files.
     * 
     * @param logId UUID of the export log entry
     * @param currentUser The authenticated tenant admin
     * @return CSV file as downloadable attachment or error response
     */
    @GetMapping("/export-logs/{logId}/download")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ByteArrayResource> downloadExportedFile(@PathVariable UUID logId, @AuthenticationPrincipal User currentUser) {
        // Retrieve export log and verify it exists
        PayrollExportLog exportLog = payrollExportLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Export log not found"));

        // Security check: Ensure user can only access their tenant's exports
        if (!exportLog.getTenant().getId().equals(currentUser.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Load file from storage and prepare for download
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