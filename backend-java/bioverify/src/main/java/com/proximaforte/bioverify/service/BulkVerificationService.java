package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.BulkVerificationJob;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.JobStatus;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.dto.BulkJobStatusDto;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.SotProfileDto;
import com.proximaforte.bioverify.repository.BulkVerificationJobRepository;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
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
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @SneakyThrows
    public void startBulkVerification(User currentUser) { // <-- MODIFIED: Simplified signature
        UUID tenantId = currentUser.getTenant().getId();
        logger.info("Starting bulk verification process for tenantId: {}", tenantId);

        // MODIFIED: Find ALL pending records for the entire tenant
        List<MasterListRecord> recordsToVerify = recordRepository.findAllByTenantIdAndStatus(tenantId, RecordStatus.PENDING_VERIFICATION);
        if (recordsToVerify.isEmpty()) {
            logger.info("No records in PENDING_VERIFICATION status for tenantId: {}. No job created.", tenantId);
            return;
        }

        BulkVerificationJob job = new BulkVerificationJob();
        job.setUploadId(null); // MODIFIED: No longer associated with a single upload
        job.setTenant(currentUser.getTenant());
        job.setInitiatedBy(currentUser);
        job.setStatus(JobStatus.PENDING);
        job.setTotalRecords(recordsToVerify.size());
        job = jobRepository.save(job);

        runVerificationJob(job, currentUser, recordsToVerify);
    }

    @Async
    @Transactional
    @SneakyThrows
    public void runVerificationJob(BulkVerificationJob job, User currentUser, List<MasterListRecord> recordsToVerify) {
        logger.info("Starting background job {} for tenantId: {}", job.getId(), job.getTenant().getId());

        try {
            job.setStatus(JobStatus.RUNNING);
            jobRepository.save(job);

            IdentitySourceConfigDto config = objectMapper.readValue(currentUser.getTenant().getidentitySourceConfig(), IdentitySourceConfigDto.class);

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

        BulkJobStatusDto initialResponse;
        try {
            initialResponse = webClient.post()
                    .uri(baseUrl + "/bulk-inquiry")
                    .header("client-id", config.getClientId())
                    .bodyValue(Map.of("psnList", psnList))
                    .retrieve()
                    .bodyToMono(BulkJobStatusDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Optima API failed to initiate job. Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
        }

        if (initialResponse == null || initialResponse.getJobId() == null) {
            throw new RuntimeException("Failed to get a valid jobId from Optima.");
        }
        job.setExternalJobId(initialResponse.getJobId());
        jobRepository.save(job);

        BulkJobStatusDto finalStatus = null;
        while (true) {
            Thread.sleep(Duration.ofSeconds(30).toMillis());
            finalStatus = webClient.get()
                    .uri(baseUrl + "/bulk-inquiry/{jobId}/status", job.getExternalJobId())
                    .header("client-id", config.getClientId())
                    .retrieve()
                    .bodyToMono(BulkJobStatusDto.class)
                    .block();
            
            if (finalStatus != null && "COMPLETED".equalsIgnoreCase(finalStatus.getStatus())) {
                break;
            } else if (finalStatus != null && "FAILED".equalsIgnoreCase(finalStatus.getStatus())) {
                throw new RuntimeException("Optima job failed with message: " + finalStatus.getMessage());
            }
        }

        if (finalStatus == null || finalStatus.getFileUrl() == null || finalStatus.getFileUrl().isBlank()) {
            throw new RuntimeException("Optima job completed but did not provide a valid file URL.");
        }

        String resultsJson = webClient.get().uri(finalStatus.getFileUrl()).retrieve().bodyToMono(String.class).block();
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