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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Map<String, MasterListRecord> existingRecordsMap = recordRepository.findAllByTenantId(tenantId)
                .stream()
                .filter(record -> record.getSsid() != null && !record.getSsid().isBlank())
                .collect(Collectors.toMap(MasterListRecord::getSsid, Function.identity())); // <-- PARENTHESES ADDED

        List<MasterListRecord> recordsToSave = new ArrayList<>();
        List<UUID> recordsRequiringReverificationIds = new ArrayList<>();
        int newRecordsCount = 0;
        int updatedRecordsCount = 0;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withTrim(true).withIgnoreEmptyLines(true);
            CSVParser csvParser = new CSVParser(reader, csvFormat);

            for (CSVRecord csvRecord : csvParser) {
                String ssid = csvRecord.get("SSID");
                if (ssid == null || ssid.isBlank()) continue;

                MasterListRecord existingRecord = existingRecordsMap.get(ssid);
                String newFullName = csvRecord.get("Full Name");
                String newGradeLevel = csvRecord.get("Grade Level");
                String newNin = csvRecord.get("NIN");

                if (existingRecord != null) {
                    updatedRecordsCount++;
                    boolean isCriticalChange = !Objects.equals(existingRecord.getFullName(), newFullName) ||
                                               !Objects.equals(existingRecord.getGradeLevel(), newGradeLevel);

                    if (isCriticalChange) {
                        existingRecord.setFullName(newFullName);
                        existingRecord.setGradeLevel(newGradeLevel);
                        existingRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                        existingRecord.setNin(newNin);
                        existingRecord.setStatus(RecordStatus.AWAITING_REVERIFICATION);
                        recordsRequiringReverificationIds.add(existingRecord.getId());
                    } else {
                        existingRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                        existingRecord.setNin(newNin);
                    }
                    recordsToSave.add(existingRecord);
                } else {
                    newRecordsCount++;
                    MasterListRecord newRecord = new MasterListRecord();
                    newRecord.setTenant(tenant);
                    newRecord.setSsid(ssid);
                    newRecord.setFullName(newFullName);
                    newRecord.setBusinessUnit(csvRecord.get("Business Unit"));
                    newRecord.setGradeLevel(newGradeLevel);
                    newRecord.setNin(newNin);
                    newRecord.setStatus(RecordStatus.PENDING);
                    recordsToSave.add(newRecord);
                }
            }
        }

        recordRepository.saveAll(recordsToSave);
        return new UploadSummaryDto(newRecordsCount, updatedRecordsCount, recordsRequiringReverificationIds);
    }
}