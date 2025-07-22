package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.*;
import com.proximaforte.bioverify.domain.enums.JobStatus;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.SotProfileDto;
import com.proximaforte.bioverify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class BulkVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(BulkVerificationService.class);
    private final MasterListRecordRepository recordRepository;
    private final BulkVerificationJobRepository jobRepository;
    private final TenantRepository tenantRepository;
    private final DepartmentRepository departmentRepository; // <-- ADDED
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @SneakyThrows
    public void startBulkVerification(User currentUser) {
        UUID tenantId = currentUser.getTenant().getId();
        logger.info("Starting bulk verification process for tenantId: {}", tenantId);

        List<MasterListRecord> recordsToVerify = recordRepository.findAllByTenantIdAndStatus(tenantId, RecordStatus.PENDING_VERIFICATION);
        if (recordsToVerify.isEmpty()) {
            logger.info("No records in PENDING_VERIFICATION status for tenantId: {}. No job created.", tenantId);
            return;
        }

        BulkVerificationJob job = new BulkVerificationJob();
        job.setTenant(currentUser.getTenant());
        job.setInitiatedBy(currentUser);
        job.setStatus(JobStatus.PENDING);
        job.setTotalRecords(recordsToVerify.size());
        job = jobRepository.save(job);

        runVerificationJob(job, tenantId, recordsToVerify);
    }

    @Async
    @Transactional
    @SneakyThrows
    public void runVerificationJob(BulkVerificationJob job, UUID tenantId, List<MasterListRecord> recordsToVerify) {
        logger.info("Starting background job {} for tenantId: {}", job.getId(), tenantId);

        try {
            job.setStatus(JobStatus.RUNNING);
            jobRepository.save(job);

            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Tenant not found for ID: " + tenantId));

            IdentitySourceConfigDto config = objectMapper.readValue(tenant.getidentitySourceConfig(), IdentitySourceConfigDto.class);

            if ("OPTIMA".equalsIgnoreCase(config.getProviderName())) {
                runOptimaBulkVerification(job, config, recordsToVerify);
            } else {
                throw new UnsupportedOperationException("Bulk verification not supported for provider: " + config.getProviderName());
            }

            job.setStatus(JobStatus.COMPLETED);
            job.setStatusMessage("Bulk verification completed successfully.");
            jobRepository.save(job);
            logger.info("Background job {} completed successfully.", job.getId());

        } catch (Exception e) {
            logger.error("Bulk verification job {} failed with an exception.", job.getId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage("Job failed: " + e.getMessage());
            jobRepository.save(job);
        }
    }

    @SneakyThrows
    private void runOptimaBulkVerification(BulkVerificationJob job, IdentitySourceConfigDto config, List<MasterListRecord> recordsToVerify) {
        List<String> psnList = recordsToVerify.stream().map(MasterListRecord::getPsn).collect(Collectors.toList());
        WebClient webClient = webClientBuilder.build();
        String baseUrl = config.getApiBaseUrl();

        try {
            JsonNode initialResponse = webClient.post()
                    .uri(baseUrl + "/bulk-inquiry")
                    .header("client-id", config.getClientId())
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("psnList", psnList))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode dataNode = initialResponse.path("data");
            String jobId = dataNode.path("jobId").asText();

            if (jobId == null || jobId.isEmpty()) {
                throw new RuntimeException("Failed to get a valid jobId from Optima's response. Data object: " + dataNode.toString());
            }
            
            logger.info("Bulk job initiated with jobId: {}", jobId);
            job.setExternalJobId(jobId);
            jobRepository.save(job);
            
            String fileUrl = null;
            while (true) {
                Thread.sleep(Duration.ofSeconds(30).toMillis());
                
                JsonNode statusResponse = webClient.get()
                        .uri(baseUrl + "/bulk-inquiry/{jobId}/status", jobId)
                        .header("client-id", config.getClientId())
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                
                JsonNode finalStatusNode = statusResponse.path("data");
                String status = finalStatusNode.path("status").asText();

                if ("COMPLETED".equalsIgnoreCase(status)) {
                    logger.info("Job {} completed successfully.", jobId);
                    
                    fileUrl = finalStatusNode.path("fileUrl").asText(null);
                    if (fileUrl == null || fileUrl.isBlank()) {
                        throw new RuntimeException("Optima job completed but did not provide a valid file URL.");
                    }
                    
                    byte[] zipBytes = webClient.get().uri(fileUrl).retrieve().bodyToMono(byte[].class).block();

                    byte[] encryptedCsvBytes = null;
                    try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        if (zipInputStream.getNextEntry() != null) {
                            encryptedCsvBytes = zipInputStream.readAllBytes();
                        }
                    }

                    if (encryptedCsvBytes == null) {
                        throw new RuntimeException("Downloaded ZIP archive was empty or did not contain a file.");
                    }

                    String decryptedCsv = decrypt(encryptedCsvBytes, config.getAesKey(), config.getIv());
                    
                    List<SotProfileDto> verifiedProfiles = new ArrayList<>();
                    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(decryptedCsv.getBytes(StandardCharsets.UTF_8)))) {
                        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim());

                        for (CSVRecord csvRecord : csvParser) {
                            SotProfileDto profile = new SotProfileDto();
                            profile.setFirstName(csvRecord.get("first_name"));
                            profile.setMiddleName(csvRecord.get("middle_name"));
                            profile.setSurname(csvRecord.get("surname"));
                            profile.setPsn(csvRecord.get("psn"));
                            profile.setGradeLevel(csvRecord.get("grade_level"));
                            profile.setStateMinistry(csvRecord.get("state_ministry"));
                            profile.setCadre(csvRecord.get("cadre"));
                            profile.setOnTransfer(Boolean.parseBoolean(csvRecord.get("on_transfer")));
                            profile.setDateOfFirstAppointment(csvRecord.get("date_of_first_appointment"));
                            profile.setDateOfConfirmation(csvRecord.get("date_of_confirmation"));
                            profile.setBvn(csvRecord.get("bvn"));
                            
                            verifiedProfiles.add(profile);
                        }
                    }

                    if (verifiedProfiles.isEmpty()) {
                        logger.warn("CSV file from ZIP archive was empty or contained no records.");
                    }
                    
                    Map<String, MasterListRecord> recordsByPsn = recordsToVerify.stream().collect(Collectors.toMap(MasterListRecord::getPsn, Function.identity()));

                    int successCount = 0;
                    for (SotProfileDto profile : verifiedProfiles) {
                        MasterListRecord recordToUpdate = recordsByPsn.get(profile.getPsn());
                        if (recordToUpdate != null) {
                            updateRecordWithSotData(recordToUpdate, profile);
                            recordRepository.save(recordToUpdate);
                            successCount++;
                        }
                        job.setProcessedRecords(job.getProcessedRecords() + 1);
                    }
                    job.setSuccessfullyVerifiedRecords(successCount);
                    job.setFailedRecords(job.getTotalRecords() - successCount);

                    return;
                
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    throw new RuntimeException("Optima job failed with message: " + finalStatusNode.path("message").asText());
                }
            }

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Optima API call failed. Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
        }
    }
    
    @SneakyThrows
    private String decrypt(byte[] cipherText, String key, String iv) {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    private void updateRecordWithSotData(MasterListRecord record, SotProfileDto profile) {
        String fullName = (profile.getFirstName() + " " + profile.getMiddleName() + " " + profile.getSurname()).replace("  ", " ").trim();
        record.setFullName(fullName);
        record.setBvn(profile.getBvn());
        record.setGradeLevel(profile.getGradeLevel());
        
        // --- UPDATED LOGIC ---
        // Use the find or create logic for the department from SoT data
        record.setDepartment(findOrCreateDepartment(profile.getStateMinistry(), record.getTenant()));
        
        record.setCadre(profile.getCadre());
        record.setOnTransfer(profile.isOnTransfer());

        try {
            String firstAppointmentStr = profile.getDateOfFirstAppointment();
            if (firstAppointmentStr != null && !firstAppointmentStr.isBlank()) {
                long firstAppointmentMillis = Long.parseLong(firstAppointmentStr);
                record.setDateOfFirstAppointment(
                    Instant.ofEpochMilli(firstAppointmentMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                );
            }

            String confirmationStr = profile.getDateOfConfirmation();
            if (confirmationStr != null && !confirmationStr.isBlank()) {
                long confirmationMillis = Long.parseLong(confirmationStr);
                record.setDateOfConfirmation(
                    Instant.ofEpochMilli(confirmationMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                );
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse date timestamp for PSN {}. Error: {}", profile.getPsn(), e.getMessage());
        }
        
        record.setStatus(RecordStatus.PENDING_GRADE_VALIDATION);
    }
    
    // --- NEW HELPER METHOD ---
    private Department findOrCreateDepartment(String name, Tenant tenant) {
        if (name == null || name.isBlank()) return null;
        return departmentRepository.findByNameAndTenantId(name, tenant.getId())
                .orElseGet(() -> {
                    Department newDept = new Department();
                    newDept.setName(name);
                    newDept.setTenant(tenant);
                    return departmentRepository.save(newDept);
                });
    }
}