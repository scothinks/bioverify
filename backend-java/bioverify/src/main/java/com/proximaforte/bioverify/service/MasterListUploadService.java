package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.RecordStatus;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.UploadSummaryDto;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class MasterListUploadService {

    private final MasterListRecordRepository recordRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public MasterListUploadService(MasterListRecordRepository recordRepository, TenantRepository tenantRepository) {
        this.recordRepository = recordRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public UploadSummaryDto processUpload(MultipartFile file, UUID tenantId) throws Exception {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant with ID " + tenantId + " not found."));

        List<MasterListRecord> recordsToSave = new ArrayList<>();
        List<UUID> recordsRequiringReverificationIds = new ArrayList<>();
        int newRecordsCount = 0;
        int updatedRecordsCount = 0;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withTrim(true).withIgnoreEmptyLines(true);
            CSVParser csvParser = new CSVParser(reader, csvFormat);

            for (CSVRecord csvRecord : csvParser) {
                String ssid = csvRecord.get("SSID");
                String nin = csvRecord.get("NIN");

                if ((ssid == null || ssid.isBlank()) || (nin == null || nin.isBlank())) {
                    continue; // Skip rows without both a valid SSID and NIN
                }

                // Hash the incoming plaintext identifiers for a fast, secure lookup
                String ssidHash = toSha256(ssid);
                String ninHash = toSha256(nin);

                Optional<MasterListRecord> existingRecordOpt = recordRepository.findBySsidHashAndNinHash(ssidHash, ninHash);
                String newFullName = csvRecord.get("Full Name");
                String newGradeLevel = csvRecord.get("Grade Level");

                if (existingRecordOpt.isPresent()) {
                    // --- UPDATE LOGIC ---
                    updatedRecordsCount++;
                    MasterListRecord existingRecord = existingRecordOpt.get();
                    
                    boolean isCriticalChange = !Objects.equals(existingRecord.getFullName(), newFullName) ||
                                               !Objects.equals(existingRecord.getGradeLevel(), newGradeLevel);

                    if (isCriticalChange) {
                        existingRecord.setFullName(newFullName);
                        existingRecord.setGradeLevel(newGradeLevel);
                        existingRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                        existingRecord.setNin(nin);
                        existingRecord.setSsid(ssid); // Also update the encrypted values
                        existingRecord.setNinHash(ninHash);
                        existingRecord.setSsidHash(ssidHash);
                        existingRecord.setStatus(RecordStatus.AWAITING_REVERIFICATION);
                        recordsRequiringReverificationIds.add(existingRecord.getId());
                    } else {
                        existingRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                    }
                    recordsToSave.add(existingRecord);

                } else {
                    // --- CREATE LOGIC ---
                    newRecordsCount++;
                    MasterListRecord newRecord = new MasterListRecord();
                    newRecord.setTenant(tenant);
                    newRecord.setSsid(ssid);
                    newRecord.setNin(nin);
                    newRecord.setSsidHash(ssidHash); // Set the hash
                    newRecord.setNinHash(ninHash); // Set the hash
                    newRecord.setFullName(newFullName);
                    newRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                    newRecord.setGradeLevel(newGradeLevel);
                    newRecord.setStatus(RecordStatus.PENDING);
                    recordsToSave.add(newRecord);
                }
            }
        }

        recordRepository.saveAll(recordsToSave);
        return new UploadSummaryDto(newRecordsCount, updatedRecordsCount, recordsRequiringReverificationIds);
    }

    /**
     * Hashing utility to convert a string to its SHA-256 hash.
     * @param input The string to hash.
     * @return The hex representation of the SHA-256 hash.
     */
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