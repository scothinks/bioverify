package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.BulkVerificationJob;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.JobStatus;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.SotProfileDto;
import com.proximaforte.bioverify.repository.BulkVerificationJobRepository;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(BulkVerificationService.class);
    private final MasterListRecordRepository recordRepository;
    private final BulkVerificationJobRepository jobRepository;
    private final TenantRepository tenantRepository;
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
                    
                    String fileUrl = finalStatusNode.path("fileUrl").asText(null);
                    if (fileUrl == null || fileUrl.isBlank()) {
                        throw new RuntimeException("Optima job completed but did not provide a valid file URL.");
                    }
                    
                    // --- LOGIC MOVED: All result processing is now safely inside the completion block ---
                    String resultsJson = webClient.get().uri(fileUrl).retrieve().bodyToMono(String.class).block();
                    List<SotProfileDto> verifiedProfiles = objectMapper.readValue(resultsJson, new TypeReference<>() {});
                    
                    Map<String, MasterListRecord> recordsByPsn = recordsToVerify.stream().collect(Collectors.toMap(MasterListRecord::getPsn, Function.identity()));

                    int successCount = 0;
                    for (SotProfileDto profile : verifiedProfiles) {
                        MasterListRecord recordToUpdate = recordsByPsn.get(profile.getCivilServiceProfile().getPsn());
                        if (recordToUpdate != null) {
                            updateRecordWithSotData(recordToUpdate, profile);
                            recordRepository.save(recordToUpdate);
                            successCount++;
                        }
                        job.setProcessedRecords(job.getProcessedRecords() + 1);
                    }
                    job.setSuccessfullyVerifiedRecords(successCount);
                    job.setFailedRecords(job.getTotalRecords() - successCount);

                    // The work is done, so we can exit the method.
                    return; 
                
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    throw new RuntimeException("Optima job failed with message: " + finalStatusNode.path("message").asText());
                }
            }

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Optima API call failed. Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
        }
    }
    
    private void updateRecordWithSotData(MasterListRecord record, SotProfileDto profile) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        record.setFullName(profile.getIdentity().getFirstName() + " " + profile.getIdentity().getMiddleName() + " " + profile.getIdentity().getSurname());
        record.setBvn(profile.getBvn());
        record.setGradeLevel(profile.getCivilServiceProfile().getGradeLevel());
        record.setBusinessUnit(profile.getCivilServiceProfile().getStateMinistry());
        record.setCadre(profile.getCivilServiceProfile().getCadre());
        record.setOnTransfer(profile.getCivilServiceProfile().isOnTransfer());
        record.setDateOfFirstAppointment(LocalDate.parse(profile.getCivilServiceProfile().getDateOfFirstAppointment(), formatter));
        record.setDateOfConfirmation(LocalDate.parse(profile.getCivilServiceProfile().getDateOfConfirmation(), formatter));
        record.setStatus(RecordStatus.PENDING_GRADE_VALIDATION);
    }
}