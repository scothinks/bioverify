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

/**
 * Implementation of Proof of Life (PoL) service handling biometric verification workflow.
 * 
 * This service orchestrates the complete PoL process including:
 * - Document validation using OCR and automated checks
 * - File storage with unique naming conventions
 * - User account creation for verified employees
 * - Work ID generation and assignment
 * - Status transitions and audit trail updates
 * - Liveness check scheduling
 * 
 * The PoL process is a critical security checkpoint that combines:
 * 1. Live photo capture for biometric verification
 * 2. Document authentication (Letter of Employment, Work ID)
 * 3. Automated validation with manual review fallback
 * 4. Account creation with email activation workflow
 */
@Service
public class ProofOfLifeServiceImpl implements ProofOfLifeService {

    // Core service dependencies for PoL operations
    private final MasterListRecordRepository recordRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeIdService employeeIdService;
    private final AuthenticationService authenticationService;
    private final DocumentValidationService documentValidationService;

    /**
     * Constructor injection for all required services.
     * Uses constructor injection pattern for better testability and immutability.
     */
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

    /**
     * Completes the Proof of Life verification process for an employee record.
     * 
     * This is the core workflow method that handles the complete PoL process:
     * 1. Validates record eligibility and status
     * 2. Performs automated document validation using OCR
     * 3. Stores all submitted files with unique identifiers
     * 4. Creates user account and generates Work ID on success
     * 5. Flags documents for manual review on validation failure
     * 6. Schedules future liveness checks
     * 
     * @param recordId UUID of the employee record being processed
     * @param email Employee's email for account creation and notifications
     * @param photo Live photo captured during PoL session
     * @param letterOfEmployment PDF document proving current employment
     * @param workId PDF document with employee identification
     * @return true if validation passes and account is created, false if flagged for review
     * @throws RuntimeException if record not found or file storage fails
     * @throws IllegalStateException if record is not in REVIEWED status
     */
    @Override
    @Transactional
    public boolean completePoL(UUID recordId, String email, MultipartFile photo, MultipartFile letterOfEmployment, MultipartFile workId) {
        try {
            // Step 1: Retrieve and validate record eligibility
            MasterListRecord record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            User agent = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // Verify record is in correct status for PoL processing
            if (record.getStatus() != RecordStatus.REVIEWED) {
                throw new IllegalStateException("Record is not awaiting Proof of Life.");
            }

            // Step 2: Prepare documents for automated validation
            Map<DocumentType, MultipartFile> documentsToValidate = new EnumMap<>(DocumentType.class);
            documentsToValidate.put(DocumentType.LETTER_OF_EMPLOYMENT, letterOfEmployment);
            documentsToValidate.put(DocumentType.WORK_ID, workId);

            // Step 3: Perform automated document validation using OCR
            boolean areDocumentsValid = documentValidationService.validateDocuments(record, documentsToValidate);

            // Path A: Document validation failed - flag for manual review
            if (!areDocumentsValid) {
                record.setStatus(RecordStatus.FLAGGED_INVALID_DOCUMENT);
                
                // Store email even on failure for potential manual approval
                record.setEmail(email);

                // Save all files for manual review by administrators
                String photoUrl = saveFileWithUniqueName(photo);
                String letterUrl = saveFileWithUniqueName(letterOfEmployment);
                String workIdUrl = saveFileWithUniqueName(workId);
                record.setPhotoUrl(photoUrl);

                // Update document URLs collection
                List<String> docUrls = record.getDocumentUrls();
                docUrls.clear();
                docUrls.add(letterUrl);
                docUrls.add(workIdUrl);

                recordRepository.save(record);
                return false; // Indicate validation failure requiring manual review
            }
            
            // Path B: Document validation successful - complete activation
            record.setEmail(email);

            // Step 4: Store all submitted files with unique names
            String photoUrl = saveFileWithUniqueName(photo);
            String letterUrl = saveFileWithUniqueName(letterOfEmployment);
            String workIdUrl = saveFileWithUniqueName(workId);
            
            // Step 5: Generate Work ID and create user account
            String wid = employeeIdService.generateNewWorkId(record.getTenant());
            User employeeUser = authenticationService.createSelfServiceAccountForRecord(record, email);

            // Step 6: Update record with PoL completion details
            record.setPolAgent(agent);                    // Track which agent performed PoL
            record.setPolPerformedAt(Instant.now());      // Timestamp of PoL completion
            record.setPhotoUrl(photoUrl);                 // Store photo reference

            // Update document URLs collection
            List<String> docUrls = record.getDocumentUrls();
            docUrls.clear();
            docUrls.add(letterUrl);
            docUrls.add(workIdUrl);

            // Step 7: Finalize record activation
            record.setWid(wid);                          // Assign unique Work ID
            record.setUser(employeeUser);                // Link to created user account
            record.setStatus(RecordStatus.ACTIVE);       // Mark as fully active
            
            // Step 8: Schedule liveness check requirements
            record.setLastLivenessCheckDate(LocalDate.now());
            record.setNextLivenessCheckDate(LocalDate.now().plusMonths(6));

            recordRepository.save(record);
            return true; // Indicate successful activation

        } catch (IOException e) {
            throw new RuntimeException("Failed to store one or more files.", e);
        }
    }
    
    /**
     * Saves an uploaded file with a unique UUID-based filename to prevent conflicts.
     * 
     * This utility method:
     * 1. Handles null/empty file validation
     * 2. Preserves original file extension for proper MIME type handling
     * 3. Generates UUID-based unique filename to prevent collisions
     * 4. Delegates actual storage to FileStorageService
     * 
     * @param file The uploaded file to be stored
     * @return Unique filename for the stored file, or null if file is empty
     * @throws IOException if file storage operation fails
     */
    private String saveFileWithUniqueName(MultipartFile file) throws IOException {
        // Handle null or empty files gracefully
        if (file == null || file.isEmpty()) return null;
        
        // Extract original file extension to maintain proper MIME types
        String originalFileName = file.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFileName);
        
        // Generate unique filename using UUID to prevent naming conflicts
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Store file using the file storage service
        fileStorageService.save(file.getBytes(), uniqueFileName);
        
        return uniqueFileName;
    }
}