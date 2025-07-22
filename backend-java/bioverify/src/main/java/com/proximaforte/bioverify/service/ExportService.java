package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.PayrollExportLog;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.JobStatus;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.PayrollExportLogRepository;
import com.proximaforte.bioverify.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ExportService {

    private final MasterListRecordRepository recordRepository;
    private final PayrollExportLogRepository logRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository; // Inject UserRepository
    private final ExportService self;

    public ExportService(MasterListRecordRepository recordRepository,
                         PayrollExportLogRepository logRepository,
                         FileStorageService fileStorageService,
                         ObjectMapper objectMapper,
                         UserRepository userRepository, // Add to constructor
                         @Lazy ExportService self) {
        this.recordRepository = recordRepository;
        this.logRepository = logRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository; // Add to constructor
        this.self = self;
    }

    @Async
    public void generateExport(User initiator) {
        // --- ADDED THIS BLOCK TO FIX THE ERROR ---
        // Re-load the user with their tenant to ensure all data is available in the async thread
        User fullInitiator = userRepository.findByIdWithTenant(initiator.getId())
            .orElseThrow(() -> new EntityNotFoundException("Initiator user not found with ID: " + initiator.getId()));
        // --- END OF FIX ---

        PayrollExportLog exportLog = self.createInitialLogEntry(fullInitiator);
        UUID logId = exportLog.getId();
        log.info("Starting new payroll export process with log ID: {}", logId);

        try {
            List<MasterListRecord> recordsToExport = self.getRecordsToExport(fullInitiator.getTenant().getId());

            if (recordsToExport.isEmpty()) {
                self.updateLogStatus(logId, JobStatus.COMPLETED, "No new records to export.", null);
                log.info("No new validated records to export for tenant: {}", fullInitiator.getTenant().getId());
                return;
            }

            log.info("Found {} records to export for tenant: {}", recordsToExport.size(), fullInitiator.getTenant().getId());

            // Use the fully loaded initiator object here
            byte[] csvData = createCsvData(recordsToExport);
            String fileName = String.format("payroll-export-%s-%s.csv", fullInitiator.getTenant().getStateCode(), logId);
            String fileUrl = fileStorageService.save(csvData, fileName);

            self.finalizeExport(logId, (long) recordsToExport.size(), fileUrl, recordsToExport);
            log.info("Successfully completed payroll export {} for tenant {}", logId, fullInitiator.getTenant().getId());

        } catch (Exception e) {
            log.error("Payroll export process for log ID {} failed for tenant: {}", logId, fullInitiator.getTenant().getId(), e);
            self.updateLogStatus(logId, JobStatus.FAILED, "Job failed: " + e.getMessage(), null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayrollExportLog createInitialLogEntry(User initiator) {
        PayrollExportLog exportLog = new PayrollExportLog();
        exportLog.setTenant(initiator.getTenant());
        exportLog.setExportedBy(initiator);
        exportLog.setExportTimestamp(Instant.now());
        exportLog.setRecordCount(0L);
        exportLog.setStatus(JobStatus.RUNNING);
        return logRepository.save(exportLog);
    }
    
    @Transactional(readOnly = true)
    public List<MasterListRecord> getRecordsToExport(UUID tenantId) {
        return recordRepository.findAllToExport(tenantId, RecordStatus.VALIDATED);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeExport(UUID logId, Long recordCount, String fileUrl, List<MasterListRecord> recordsToExport) {
        PayrollExportLog finalLog = logRepository.findById(logId)
            .orElseThrow(() -> new EntityNotFoundException("PayrollExportLog not found with ID: " + logId));

        finalLog.setRecordCount(recordCount);
        finalLog.setFileUrl(fileUrl);
        finalLog.setStatus(JobStatus.COMPLETED);
        finalLog.setStatusMessage("Export completed successfully.");

        for (MasterListRecord record : recordsToExport) {
            record.setPayrollExportLog(finalLog);
        }
        recordRepository.saveAll(recordsToExport);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLogStatus(UUID logId, JobStatus status, String message, String fileUrl) {
        PayrollExportLog failedLog = logRepository.findById(logId)
            .orElseThrow(() -> new EntityNotFoundException("PayrollExportLog not found with ID: " + logId));
        
        failedLog.setStatus(status);
        failedLog.setStatusMessage(message);
        failedLog.setFileUrl(fileUrl);
        logRepository.save(failedLog);
    }

    private byte[] createCsvData(List<MasterListRecord> records) throws IOException {
        List<String> verifiedHeaders = List.of("employeeId", "status", "validatedAt", "validatedByEmail");
        List<Map<String, String>> processedRecords = new ArrayList<>();
        List<String> allHeaders = new ArrayList<>();
        for (MasterListRecord record : records) {
            Map<String, String> originalData;
            try {
                originalData = objectMapper.readValue(record.getOriginalUploadData(), new TypeReference<>() {});
            } catch (Exception e) {
                originalData = new LinkedHashMap<>();
            }
            originalData.put("employeeId", record.getEmployeeId());
            originalData.put("status", record.getStatus().toString());
            originalData.put("validatedAt", record.getValidatedAt() != null ? record.getValidatedAt().toString() : "");
            originalData.put("validatedByEmail", record.getValidatedBy() != null ? record.getValidatedBy().getEmail() : "");
            if (record.getSotData() != null) {}
            processedRecords.add(originalData);
            for (String key : originalData.keySet()) {
                if (!allHeaders.contains(key)) {
                    allHeaders.add(key);
                }
            }
        }
        for (String header : verifiedHeaders) {
            if (!allHeaders.contains(header)) {
                allHeaders.add(header);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(allHeaders.toArray(new String[0])))) {
            for (Map<String, String> recordMap : processedRecords) {
                csvPrinter.printRecord(allHeaders.stream().map(h -> recordMap.getOrDefault(h, "")));
            }
        }
        return out.toByteArray();
    }
}