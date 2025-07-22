package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.PayrollExportLog;
import com.proximaforte.bioverify.domain.enums.JobStatus; // 1. Import JobStatus
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PayrollExportLogDto {

    private UUID id;
    private Instant exportTimestamp;
    private Long recordCount;
    private String exportedByEmail;
    private JobStatus status; // 2. Add status field
    private String statusMessage; // 3. Add statusMessage field

    public PayrollExportLogDto(PayrollExportLog log) {
        this.id = log.getId();
        this.exportTimestamp = log.getExportTimestamp();
        this.recordCount = log.getRecordCount();
        this.status = log.getStatus(); // 4. Map the status
        this.statusMessage = log.getStatusMessage(); // 5. Map the message
        
        if (log.getExportedBy() != null) {
            this.exportedByEmail = log.getExportedBy().getEmail();
        } else {
            this.exportedByEmail = "N/A";
        }
    }
}