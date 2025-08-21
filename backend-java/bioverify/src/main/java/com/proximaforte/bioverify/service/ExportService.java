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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        // --- SIMPLIFIED LOGIC: This list is now the single source of truth for headers and their order. ---
        List<String> finalHeaders = Arrays.asList(
            "workId", "firstName", "middleName", "surname", "gradeLevel", "salaryStructure",
            "ministry", "department", "email", "phoneNumber", "status", "bvn", "nin", "ssid",
            "psn", "dateOfBirth", "gender", "cadre", "dateOfFirstAppointment",
            "validatedByEmail", "validatedAt"
        );

        List<Map<String, String>> processedRecords = new ArrayList<>();
        
        for (MasterListRecord record : records) {
            Map<String, String> recordMap = new LinkedHashMap<>();

            // Populate the map directly from the verified entity data
            recordMap.put("workId", record.getWid() != null ? record.getWid() : "");
            recordMap.put("gradeLevel", record.getGradeLevel() != null ? record.getGradeLevel() : "");
            recordMap.put("salaryStructure", record.getSalaryStructure() != null ? record.getSalaryStructure() : "");
            recordMap.put("ministry", record.getMinistry() != null ? record.getMinistry().getName() : "");
            recordMap.put("department", record.getDepartment() != null ? record.getDepartment().getName() : "");
            recordMap.put("email", record.getEmail() != null ? record.getEmail() : "");
            recordMap.put("phoneNumber", record.getPhoneNumber() != null ? record.getPhoneNumber() : "");
            recordMap.put("status", record.getStatus() != null ? record.getStatus().toString() : "");
            recordMap.put("bvn", record.getBvn() != null ? record.getBvn() : "");
            recordMap.put("nin", record.getNin() != null ? record.getNin() : "");
            recordMap.put("ssid", record.getSsid() != null ? record.getSsid() : "");
            recordMap.put("psn", record.getPsn() != null ? record.getPsn() : "");
            recordMap.put("dateOfBirth", record.getDateOfBirth() != null ? record.getDateOfBirth().toString() : "");
            recordMap.put("gender", record.getGender() != null ? record.getGender() : "");
            recordMap.put("cadre", record.getCadre() != null ? record.getCadre() : "");
            recordMap.put("dateOfFirstAppointment", record.getDateOfFirstAppointment() != null ? record.getDateOfFirstAppointment().toString() : "");
            recordMap.put("validatedByEmail", record.getValidatedBy() != null ? record.getValidatedBy().getEmail() : "");
            recordMap.put("validatedAt", record.getValidatedAt() != null ? record.getValidatedAt().toString() : "");

            // Split the definitive full name from the entity into parts for the export
            String fullName = record.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                String[] names = fullName.split("\\s+");
                recordMap.put("firstName", names.length > 0 ? names[0] : "");
                if (names.length > 2) {
                    String middleName = String.join(" ", Arrays.copyOfRange(names, 1, names.length - 1));
                    recordMap.put("middleName", middleName);
                } else {
                    recordMap.put("middleName", "");
                }
                recordMap.put("surname", names.length > 1 ? names[names.length - 1] : "");
            }
            
            processedRecords.add(recordMap);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(finalHeaders.toArray(new String[0])))) {

            for (Map<String, String> recordMap : processedRecords) {
                // Print values in the order defined by finalHeaders
                csvPrinter.printRecord(finalHeaders.stream().map(h -> recordMap.getOrDefault(h, "")));
            }
        }
        return out.toByteArray();
    }
}