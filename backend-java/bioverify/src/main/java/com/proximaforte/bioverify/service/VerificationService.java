package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus;  // Fixed import
import com.proximaforte.bioverify.dto.SotProfileDto;
import com.proximaforte.bioverify.dto.VerificationResultDto;
import com.proximaforte.bioverify.exception.RecordNotFoundException;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service to orchestrate the employee verification process against a Source of Truth.
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final SotLookupService sotLookupService;
    private final MasterListRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    /**
     * The main entry point for verification. Orchestrates the call to the SoT
     * and handles the success or failure of that API call.
     */
    @Transactional
    public VerificationResultDto initiateVerification(UUID tenantId, UUID recordId, String ssid, String nin) {
        MasterListRecord record = recordRepository.findByIdAndTenantId(recordId, tenantId)
                .orElseThrow(() -> new RecordNotFoundException("Record with ID " + recordId + " not found for this tenant."));

        // Stage 2: Call the SoT provider via our dynamic lookup service
        sotLookupService.getProfile(tenantId, ssid, nin)
                .doOnSuccess(sotProfile -> {
                    // Stage 3: On success, begin Data Fusion & Reconciliation
                    processSuccessfulVerification(record, sotProfile);
                })
                .doOnError(WebClientResponseException.NotFound.class, error -> {
                    // Handle the case where the user is not found in the SoT
                    processNotFoundVerification(record);
                })
                .block(); // .block() waits for the reactive call to finish before proceeding

        return new VerificationResultDto(record.getId(), record.getStatus(), "Verification process completed.");
    }

    /**
     * Handles the logic for a successful (200 OK) response from the SoT API.
     */
    private void processSuccessfulVerification(MasterListRecord record, SotProfileDto sotProfile) {
        String newSsid = sotProfile.getSsid();
        String newNin = sotProfile.getNin();

        record.setSotData(convertObjectToJson(sotProfile));
        record.setSsid(newSsid);
        record.setNin(newNin);
        record.setSsidHash(toSha256(newSsid));
        record.setNinHash(toSha256(newNin));
        
        // This assumes SotProfileDto has nested objects as designed.
        // You would expand this with actual getters from your final DTO.
        // record.setFullName(sotProfile.getIdentity().getFirstName() + " " + sotProfile.getIdentity().getSurname());
        // record.setGradeLevel(sotProfile.getCivilServiceProfile().getGradeLevel());
        record.setVerifiedAt(Instant.now());

        boolean isMatch = performReconciliation(record.getOriginalUploadData(), sotProfile);

        if (isMatch) {
            record.setStatus(RecordStatus.PENDING_GRADE_VALIDATION);
        } else {
            record.setStatus(RecordStatus.FLAGGED_DATA_MISMATCH);
        }
        recordRepository.save(record);
    }
    
    /**
     * Handles the logic for a 404 Not Found response from the SoT API.
     */
    private void processNotFoundVerification(MasterListRecord record) {
        record.setStatus(RecordStatus.FLAGGED_NOT_IN_SOT);
        recordRepository.save(record);
    }

    /**
     * Compares key fields from the original uploaded data with the trusted SoT data.
     */
    @SneakyThrows
    private boolean performReconciliation(String originalUploadJson, SotProfileDto sotProfile) {
        if (originalUploadJson == null || originalUploadJson.isBlank()) {
            return true; // No original data to compare against
        }
        Map<String, String> originalData = objectMapper.readValue(originalUploadJson, Map.class);
        String originalPsn = originalData.getOrDefault("PSN", "");
        
        // Assuming SotProfileDto has a nested object for civilServiceProfile
        // String sotPsn = sotProfile.getCivilServiceProfile().getPsn();

        // return Objects.equals(originalPsn, sotPsn);
        return true; // Placeholder until SotProfileDto is fully fleshed out
    }

    /**
     * Hashing utility to convert a string to its SHA-256 hash.
     */
    private String toSha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find SHA-256 algorithm", e);
        }
    }
    
    @SneakyThrows
    private String convertObjectToJson(Object object) {
        return objectMapper.writeValueAsString(object);
    }
}