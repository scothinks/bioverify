package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.RecordStatus;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.dto.VerificationRequest;
import com.proximaforte.bioverify.dto.VerificationResponse;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        String ssidHash = toSha256(request.getSsid());
        String ninHash = toSha256(request.getNin());

        Optional<MasterListRecord> recordOptional = recordRepository.findBySsidHashAndNinHash(ssidHash, ninHash);
        if (recordOptional.isEmpty()) {
            return new VerificationResponse(false, "No record found with the provided SSID and NIN in our system.", null);
        }
        MasterListRecord record = recordOptional.get();

        if (!record.getTenant().getId().equals(tenantId)) {
            return new VerificationResponse(false, "Record does not belong to the current tenant.", null);
        }

        Optional<Map<String, String>> trustedDataOpt = callTenantValidationApi(request.getSsid(), request.getNin(), record.getTenant());

        if (trustedDataOpt.isPresent()) {
            Map<String, String> trustedData = trustedDataOpt.get();
            record.setFullName(trustedData.get("fullName"));
            record.setGradeLevel(trustedData.get("gradeLevel"));
            record.setBusinessUnit(trustedData.get("businessUnit"));
            record.setNin(request.getNin());
            record.setStatus(RecordStatus.VERIFIED_PENDING_CONFIRMATION);
            
            MasterListRecord savedRecord = recordRepository.save(record);
            MasterListRecordDto recordDto = new MasterListRecordDto(
                savedRecord.getId(), savedRecord.getFullName(), savedRecord.getBusinessUnit(),
                savedRecord.getGradeLevel(), savedRecord.getStatus(), savedRecord.getCreatedAt()
            );
            return new VerificationResponse(true, "Verification successful. Please review your details.", recordDto);
        } else {
            record.setStatus(RecordStatus.FLAGGED_SSID_NIN_MISMATCH);
            recordRepository.save(record);
            return new VerificationResponse(false, "SSID and NIN do not match the trusted registry.", null);
        }
    }

    @Transactional
    public MasterListRecord confirmVerification(UUID recordId) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));
        record.setStatus(RecordStatus.VERIFIED);
        return recordRepository.save(record);
    }
    
    // --- NEW METHOD for Proof of Life ---
    @Transactional
    public MasterListRecord performLivenessCheck(UUID recordId) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));

        // Update the proof of life date to the current date
        record.setLastProofOfLifeDate(LocalDate.now());

        return recordRepository.save(record);
    }

    public Optional<Map<String, String>> callTenantValidationApi(String ssid, String nin, Tenant tenant) {
        try {
            JsonNode config = objectMapper.readTree(tenant.getidentitySourceConfig());
            String apiUrl = config.path("validationUrl").asText();
            if (apiUrl == null || apiUrl.isBlank()) {
                throw new IllegalStateException("Tenant " + tenant.getName() + " does not have a validation API URL configured.");
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("ssid", ssid)
                    .queryParam("nin", nin);
            
            ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
            
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank() || responseBody.equals("{}")) {
                return Optional.empty();
            }
            
            Map<String, String> mappedData = mapApiData(responseBody, config.path("fieldMappings"));
            
            if (mappedData.get("fullName") == null || mappedData.get("fullName").isBlank()) {
                return Optional.empty();
            }

            return Optional.of(mappedData);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Error calling tenant validation API for " + tenant.getName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> mapApiData(String responseBody, JsonNode mappingConfig) throws Exception {
        Map<String, String> standardData = new HashMap<>();
        JsonNode apiData = objectMapper.readTree(responseBody);

        String fullNameField = mappingConfig.path("fullName").asText("fullName");
        String gradeLevelField = mappingConfig.path("gradeLevel").asText("gradeLevel");
        String businessUnitField = mappingConfig.path("businessUnit").asText("businessUnit");
        
        standardData.put("fullName", apiData.path(fullNameField).asText(null));
        standardData.put("gradeLevel", apiData.path(gradeLevelField).asText(null));
        standardData.put("businessUnit", apiData.path(businessUnitField).asText(null));

        return standardData;
    }

    private String toSha256(String input) {
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
}