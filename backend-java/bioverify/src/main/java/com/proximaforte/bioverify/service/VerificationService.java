package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.RecordStatus;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.VerificationRequest;
import com.proximaforte.bioverify.dto.VerificationResponse;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to handle identity verification in a true multi-tenant fashion.
 * It dynamically uses the correct API endpoint for each tenant.
 */
@Service
public class VerificationService {

    private final MasterListRecordRepository recordRepository;
    private final TenantRepository tenantRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public VerificationService(MasterListRecordRepository recordRepository, TenantRepository tenantRepository) {
        this.recordRepository = recordRepository;
        this.tenantRepository = tenantRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public VerificationResponse verifyIdentity(VerificationRequest request, UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found with ID: " + tenantId));

        Optional<MasterListRecord> recordOptional = findRecordByPlainTextSsid(request.getSsid(), tenantId);

        if (recordOptional.isEmpty()) {
            return new VerificationResponse(false, "No record found with the provided SSID for this tenant in our system.", null);
        }
        MasterListRecord record = recordOptional.get();

        boolean isRegistryMatch = callTenantValidationApi(request.getSsid(), request.getNin(), tenant);

        if (isRegistryMatch) {
            record.setNin(request.getNin());
            record.setStatus(RecordStatus.VERIFIED_PENDING_CONFIRMATION);
            MasterListRecord savedRecord = recordRepository.save(record);
            return new VerificationResponse(true, "Verification successful. Please review your details.", savedRecord);
        } else {
            record.setStatus(RecordStatus.FLAGGED_SSID_NIN_MISMATCH);
            recordRepository.save(record);
            return new VerificationResponse(false, "SSID and NIN do not match the trusted registry.", null);
        }
    }

    /**
     * Finds a record by its ID, updates its status to VERIFIED, and saves it.
     * @param recordId The ID of the record to confirm.
     * @return The updated MasterListRecord.
     */
    @Transactional
    public MasterListRecord confirmVerification(UUID recordId) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));

        record.setStatus(RecordStatus.VERIFIED);
        return recordRepository.save(record);
    }
    
    private Optional<MasterListRecord> findRecordByPlainTextSsid(String plainTextSsid, UUID tenantId) {
        List<MasterListRecord> tenantRecords = recordRepository.findAllByTenantId(tenantId);
        return tenantRecords.stream()
                .filter(record -> Objects.equals(record.getSsid(), plainTextSsid))
                .findFirst();
    }

    /**
     * Calls the validation API specific to the tenant.
     */
    public boolean callTenantValidationApi(String ssid, String nin, Tenant tenant) { // <-- CHANGED TO PUBLIC
        try {
            IdentitySourceConfigDto config = objectMapper.readValue(tenant.getidentitySourceConfig(), IdentitySourceConfigDto.class);
            String apiUrl = config.getValidationUrl();

            if (apiUrl == null || apiUrl.isBlank()) {
                throw new IllegalStateException("Tenant " + tenant.getName() + " does not have a validation API URL configured.");
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("ssid", ssid)
                    .queryParam("nin", nin);

            restTemplate.getForEntity(builder.toUriString(), String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            System.err.println("Error calling tenant validation API for " + tenant.getName() + ": " + e.getMessage());
            return false;
        }
    }
}