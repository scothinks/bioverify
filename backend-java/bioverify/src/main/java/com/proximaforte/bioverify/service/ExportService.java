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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ExportService {

    private final MasterListRecordRepository recordRepository;
    private final PayrollExportLogRepository logRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ExportService self;

    public ExportService(MasterListRecordRepository recordRepository,
                         PayrollExportLogRepository logRepository,
                         FileStorageService fileStorageService,
                         ObjectMapper objectMapper,
                         UserRepository userRepository,
                         @Lazy ExportService self) {
        this.recordRepository = recordRepository;
        this.logRepository = logRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.self = self;
    }

    @Async
    public void generateExport(User initiator) {
        User fullInitiator = userRepository.findByIdWithTenant(initiator.getId())
            .orElseThrow(() -> new EntityNotFoundException("Initiator user not found with ID: " + initiator.getId()));

        PayrollExportLog exportLog = self.createInitialLogEntry(fullInitiator);
        UUID logId = exportLog.getId();
        log.info("Starting new payroll export process with log ID: {}", logId);

        try {
            List<MasterListRecord> recordsToExport = self.getRecordsToExport(fullInitiator.getTenant().getId());

            if (recordsToExport.isEmpty()) {
                self.updateLogStatus(logId, JobStatus.COMPLETED, "No active records to export.", null);
                log.info("No active records to export for tenant: {}", fullInitiator.getTenant().getId());
                return;
            }

            log.info("Found {} records to export for tenant: {}", recordsToExport.size(), fullInitiator.getTenant().getId());

            byte[] csvData = createCsvData(recordsToExport);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
            String timestamp = formatter.format(exportLog.getExportTimestamp());
            String fileName = String.format("payroll-export-%s-%s.csv", fullInitiator.getTenant().getStateCode(), timestamp);

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
        // --- CORRECTED LOGIC ---
        // Changed RecordStatus.VALIDATED to RecordStatus.ACTIVE
        return recordRepository.findAllToExport(tenantId, RecordStatus.ACTIVE);
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
        List<Map<String, String>> processedRecords = new ArrayList<>();
        Set<String> headerSet = new LinkedHashSet<>();

        headerSet.add("employeeId");
        headerSet.add("ssid");
        headerSet.add("nin");
        headerSet.add("status");
        headerSet.add("validatedByEmail");
        headerSet.add("validatedAt");
        
        for (MasterListRecord record : records) {
             try {
                Map<String, String> originalData = objectMapper.readValue(record.getOriginalUploadData(), new TypeReference<>() {});
                headerSet.addAll(originalData.keySet());
            } catch (Exception e) {
                // Ignore
            }
        }

        for (MasterListRecord record : records) {
            Map<String, String> fusedRecord;
            try {
                fusedRecord = objectMapper.readValue(record.getOriginalUploadData(), new TypeReference<>() {});
            } catch (Exception e) {
                fusedRecord = new LinkedHashMap<>();
            }
            
            fusedRecord.put("employeeId", record.getEmployeeId() != null ? record.getEmployeeId() : "");
            fusedRecord.put("ssid", record.getSsid() != null ? record.getSsid() : "");
            fusedRecord.put("nin", record.getNin() != null ? record.getNin() : "");
            fusedRecord.put("status", record.getStatus().toString());
            fusedRecord.put("validatedAt", record.getValidatedAt() != null ? record.getValidatedAt().toString() : "");
            fusedRecord.put("validatedByEmail", record.getValidatedBy() != null ? record.getValidatedBy().getEmail() : "");
            
            processedRecords.add(fusedRecord);
        }
        
        List<String> finalHeaders = new ArrayList<>(headerSet);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(finalHeaders.toArray(new String[0])))) {

            for (Map<String, String> recordMap : processedRecords) {
                csvPrinter.printRecord(finalHeaders.stream().map(h -> recordMap.getOrDefault(h, "")));
            }
        }
        return out.toByteArray();
    }
}