package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.DocumentType;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.service.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProofOfLifeServiceImpl implements ProofOfLifeService {

    private final MasterListRecordRepository recordRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeIdService employeeIdService;
    private final AuthenticationService authenticationService;
    private final DocumentValidationService documentValidationService;

    public ProofOfLifeServiceImpl(MasterListRecordRepository recordRepository,
                                  FileStorageService fileStorageService,
                                  EmployeeIdService employeeIdService,
                                  AuthenticationService authenticationService,
                                  DocumentValidationService documentValidationService) {
        this.recordRepository = recordRepository;
        this.fileStorageService = fileStorageService;
        this.employeeIdService = employeeIdService;
        this.authenticationService = authenticationService;
        this.documentValidationService = documentValidationService;
    }

    @Override
    @Transactional
    public boolean completePoL(UUID recordId, String email, MultipartFile photo, MultipartFile letterOfEmployment, MultipartFile workId) {
        try {
            MasterListRecord record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            User agent = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (record.getStatus() != RecordStatus.REVIEWED) {
                throw new IllegalStateException("Record is not awaiting Proof of Life.");
            }

            Map<DocumentType, MultipartFile> documentsToValidate = new EnumMap<>(DocumentType.class);
            documentsToValidate.put(DocumentType.LETTER_OF_EMPLOYMENT, letterOfEmployment);
            documentsToValidate.put(DocumentType.WORK_ID, workId);

            boolean areDocumentsValid = documentValidationService.validateDocuments(record, documentsToValidate);

            if (!areDocumentsValid) {
                record.setStatus(RecordStatus.FLAGGED_INVALID_DOCUMENT);
                
                // NEW: Save the agent-provided email even on failure
                record.setEmail(email);

                String photoUrl = saveFileWithUniqueName(photo);
                String letterUrl = saveFileWithUniqueName(letterOfEmployment);
                String workIdUrl = saveFileWithUniqueName(workId);
                record.setPhotoUrl(photoUrl);

                List<String> docUrls = record.getDocumentUrls();
                docUrls.clear();
                docUrls.add(letterUrl);
                docUrls.add(workIdUrl);

                recordRepository.save(record);
                return false; // Return false on validation failure
            }
            
            record.setEmail(email);

            String photoUrl = saveFileWithUniqueName(photo);
            String letterUrl = saveFileWithUniqueName(letterOfEmployment);
            String workIdUrl = saveFileWithUniqueName(workId);
            
            String wid = employeeIdService.generateNewWorkId(record.getTenant());
            User employeeUser = authenticationService.createSelfServiceAccountForRecord(record, email);

            record.setPolAgent(agent);
            record.setPolPerformedAt(Instant.now());
            record.setPhotoUrl(photoUrl);

            List<String> docUrls = record.getDocumentUrls();
            docUrls.clear();
            docUrls.add(letterUrl);
            docUrls.add(workIdUrl);

            record.setWid(wid);
            record.setUser(employeeUser);
            record.setStatus(RecordStatus.ACTIVE);
            
            record.setLastLivenessCheckDate(LocalDate.now());
            record.setNextLivenessCheckDate(LocalDate.now().plusMonths(6));

            recordRepository.save(record);
            return true; // Return true on success

        } catch (IOException e) {
            throw new RuntimeException("Failed to store one or more files.", e);
        }
    }
    
    private String saveFileWithUniqueName(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String originalFileName = file.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        fileStorageService.save(file.getBytes(), uniqueFileName);
        
        return uniqueFileName;
    }
}