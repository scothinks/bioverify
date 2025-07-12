package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Tenant;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service to handle the business logic of uploading and processing a master list file.
 */
@Service
public class MasterListUploadService {

    private final MasterListRecordRepository recordRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public MasterListUploadService(MasterListRecordRepository recordRepository, TenantRepository tenantRepository) {
        this.recordRepository = recordRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Processes a CSV file upload, parses it, and saves the records to the database.
     * @param file The uploaded CSV file.
     * @param tenantId The ID of the tenant to which the records belong.
     * @return The number of records successfully saved.
     * @throws Exception if the tenant is not found or if there's a file processing error.
     */
    @Transactional
    public int processUpload(MultipartFile file, UUID tenantId) throws Exception {
        // First, find the tenant. If not found, we can't proceed.
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant with ID " + tenantId + " not found."));

        List<MasterListRecord> recordsToSave = new ArrayList<>();

        // Use a try-with-resources block to ensure the reader is closed automatically.
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Configure the CSV parser to expect a header row.
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("Full Name", "Grade Level", "Business Unit", "SSID")
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

            CSVParser csvParser = new CSVParser(reader, csvFormat);

            for (CSVRecord csvRecord : csvParser) {
                MasterListRecord record = new MasterListRecord();
                record.setTenant(tenant); // Associate with the correct tenant.
                record.setFullName(csvRecord.get("Full Name"));
                record.setGradeLevel(csvRecord.get("Grade Level"));
                record.setBusinessUnit(csvRecord.get("Business Unit"));
                
                // SSID is optional, so we check if the column exists and has a value.
                if (csvRecord.isMapped("SSID") && csvRecord.get("SSID") != null && !csvRecord.get("SSID").isEmpty()) {
                    record.setSsid(csvRecord.get("SSID"));
                }
                
                recordsToSave.add(record);
            }
        }

        // Save all the parsed records to the database in a single transaction.
        recordRepository.saveAll(recordsToSave);

        return recordsToSave.size();
    }
}
