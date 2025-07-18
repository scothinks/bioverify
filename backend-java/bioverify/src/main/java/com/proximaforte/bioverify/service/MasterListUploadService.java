package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.dto.UploadSummaryDto;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MasterListUploadService {

    private final MasterListRecordRepository recordRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    // Updated aliases to focus on PSN as the key identifier for uploads
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
        "psn", List.of("psn", "publicservicenumber"),
        "fullName", List.of("fullname", "full name", "employeename"),
        "surname", List.of("surname"),
        "firstName", List.of("firstname", "first name"),
        "middleName", List.of("middlename", "middle name"),
        "businessUnit", List.of("businessunit", "mda", "department", "agency"),
        "gradeLevel", List.of("gradelevel", "grade")
    );

    @Transactional
    @SneakyThrows
    public UploadSummaryDto processUpload(MultipartFile file, UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant with ID " + tenantId + " not found."));

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        
        List<Map<String, String>> data;

        if (filename.toLowerCase().endsWith(".csv")) {
            data = parseCsvData(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx")) {
            data = parseExcelData(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a CSV or Excel file.");
        }
        
        return saveData(data, tenant);
    }

    @SneakyThrows
    private List<Map<String, String>> parseCsvData(InputStream inputStream) {
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader().setTrim(true).setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).build();
            CSVParser csvParser = new CSVParser(reader, csvFormat);
            for (CSVRecord csvRecord : csvParser) {
                data.add(csvRecord.toMap());
            }
        }
        return data;
    }
    
    @SneakyThrows
    private List<Map<String, String>> parseExcelData(InputStream inputStream) {
        List<Map<String, String>> data = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            
            sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null) {
                        rowData.put(headers.get(j), dataFormatter.formatCellValue(cell));
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }

    @Transactional
    @SneakyThrows
    private UploadSummaryDto saveData(List<Map<String, String>> data, Tenant tenant) {
        if (data.isEmpty()) {
            return new UploadSummaryDto(0, 0, Collections.emptyList());
        }
        
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        Map<String, String> discoveredHeaders = findHeaderMappings(headers);

        List<MasterListRecord> recordsToSave = new ArrayList<>();
        List<UUID> recordsRequiringRevalidationIds = new ArrayList<>();
        int newRecordsCount = 0;
        int updatedRecordsCount = 0;
        
        for (Map<String, String> rowMap : data) {
            String originalUploadDataJson = objectMapper.writeValueAsString(rowMap);
            String psn = rowMap.get(discoveredHeaders.get("psn"));

            if (psn == null || psn.isBlank()) {
                continue;
            }

            // HASH THE PSN FOR SECURE LOOKUP
            String psnHash = toSha256(psn);

            // LOOKUP BY HASHED PSN
            Optional<MasterListRecord> existingRecordOpt = recordRepository.findByPsnHashAndTenantId(psnHash, tenant.getId());
            String gradeLevel = rowMap.get(discoveredHeaders.get("gradeLevel"));

            if (existingRecordOpt.isPresent()) {
                updatedRecordsCount++;
                MasterListRecord existingRecord = existingRecordOpt.get();
                existingRecord.setOriginalUploadData(originalUploadDataJson);
                boolean isCriticalChange = !Objects.equals(existingRecord.getGradeLevel(), gradeLevel);
                if (isCriticalChange && existingRecord.getStatus() == RecordStatus.VALIDATED) {
                    existingRecord.setStatus(RecordStatus.AWAITING_REVALIDATION);
                    recordsRequiringRevalidationIds.add(existingRecord.getId());
                }
                recordsToSave.add(existingRecord);
            } else {
                newRecordsCount++;
                MasterListRecord newRecord = new MasterListRecord();
                newRecord.setOriginalUploadData(originalUploadDataJson);
                newRecord.setTenant(tenant);
                
                // Set both the (encrypted) PSN and its hash
                newRecord.setPsn(psn);
                newRecord.setPsnHash(psnHash);
                
                String fullName = discoveredHeaders.containsKey("fullName") 
                    ? rowMap.get(discoveredHeaders.get("fullName"))
                    : Stream.of(rowMap.get(discoveredHeaders.get("surname")), rowMap.get(discoveredHeaders.get("firstName")), rowMap.get(discoveredHeaders.get("middleName")))
                            .filter(Objects::nonNull).map(String::trim).collect(Collectors.joining(" "));

                newRecord.setFullName(fullName);
                newRecord.setBusinessUnit(rowMap.get(discoveredHeaders.get("businessUnit")));
                newRecord.setGradeLevel(gradeLevel);
                newRecord.setStatus(RecordStatus.PENDING_VERIFICATION);
                recordsToSave.add(newRecord);
            }
        }
        
        recordRepository.saveAll(recordsToSave);
        return new UploadSummaryDto(newRecordsCount, updatedRecordsCount, recordsRequiringRevalidationIds);
    }
    
    private Map<String, String> findHeaderMappings(List<String> fileHeaders) {
        Map<String, String> mapping = new HashMap<>();
        Set<String> normalizedFileHeaders = fileHeaders.stream()
                .map(h -> h.replaceAll("\\s+", "").toLowerCase())
                .collect(Collectors.toSet());

        HEADER_ALIASES.forEach((internalField, aliases) -> {
            for (String alias : aliases) {
                if (normalizedFileHeaders.contains(alias)) {
                    String originalHeader = fileHeaders.stream()
                        .filter(h -> h.replaceAll("\\s+", "").equalsIgnoreCase(alias))
                        .findFirst().orElse(null);
                    if (originalHeader != null) {
                        mapping.put(internalField, originalHeader);
                        break;
                    }
                }
            }
        });

        // The only required column for upload is now the PSN
        if (!mapping.containsKey("psn")) {
            throw new IllegalArgumentException("Uploaded file must contain a column for PSN.");
        }
        return mapping;
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
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}