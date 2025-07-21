package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.BulkVerificationJob;
import com.proximaforte.bioverify.domain.enums.JobStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BulkJobDto {
    private UUID id;
    // --- REMOVED: The uploadId field is no longer needed ---
    private JobStatus status;
    private String statusMessage;
    private int totalRecords;
    private int processedRecords;
    private int successfullyVerifiedRecords;
    private int failedRecords;
    private Instant createdAt;
    private Instant updatedAt;

    public BulkJobDto(BulkVerificationJob job) {
        this.id = job.getId();
        this.status = job.getStatus();
        this.statusMessage = job.getStatusMessage();
        this.totalRecords = job.getTotalRecords();
        this.processedRecords = job.getProcessedRecords();
        this.successfullyVerifiedRecords = job.getSuccessfullyVerifiedRecords();
        this.failedRecords = job.getFailedRecords();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
    }
}