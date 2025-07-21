package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.dto.UpdateRecordRequestDto;
import com.proximaforte.bioverify.dto.ValidateRecordRequestDto;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MasterListRecordService {

    private final MasterListRecordRepository recordRepository;

    /**
     * Fetches records that are ready for human review.
     */
    public List<MasterListRecord> getValidationQueue(UUID tenantId) {
        return recordRepository.findByTenantIdAndStatusIn(tenantId,
                List.of(RecordStatus.PENDING_GRADE_VALIDATION, RecordStatus.FLAGGED_DATA_MISMATCH));
    }

    /**
     * Allows a reviewer to update certain fields of a record.
     */
    @Transactional
    public MasterListRecord updateRecordData(UUID recordId, UpdateRecordRequestDto request, User reviewer) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Record not found with ID: " + recordId));

        // Update only the fields provided in the request
        if (request.getFullName() != null) record.setFullName(request.getFullName());
        if (request.getDepartment() != null) record.setDepartment(request.getDepartment());
        if (request.getMinistry() != null) record.setMinistry(request.getMinistry());
        if (request.getGradeLevel() != null) record.setGradeLevel(request.getGradeLevel());
        if (request.getSalaryStructure() != null) record.setSalaryStructure(request.getSalaryStructure());
        if (request.getDateOfBirth() != null) record.setDateOfBirth(request.getDateOfBirth());

        // Set the user who performed this update for logging purposes
        record.setLastUpdatedBy(reviewer);

        return recordRepository.save(record);
    }

    /**
     * Sets the final status of a record to VALIDATED or REJECTED.
     */
    @Transactional
    public MasterListRecord performValidation(UUID recordId, ValidateRecordRequestDto request, User reviewer) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Record not found with ID: " + recordId));

        RecordStatus decision = request.getDecision();
        if (decision != RecordStatus.VALIDATED && decision != RecordStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be either VALIDATED or REJECTED.");
        }

        record.setStatus(decision);
        record.setValidatedBy(reviewer);
        record.setValidatedAt(Instant.now());

        return recordRepository.save(record);
    }

    @Transactional
    public MasterListRecord updatePsnForRecord(UUID recordId, String newPsn) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));

        record.setPsn(newPsn);
        record.setPsnHash(toSha256(newPsn));

        return recordRepository.save(record);
    }

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
}