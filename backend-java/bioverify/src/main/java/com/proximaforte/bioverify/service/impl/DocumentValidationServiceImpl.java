package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.DocumentType;
import com.proximaforte.bioverify.service.DocumentValidationService;
import com.proximaforte.bioverify.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentValidationServiceImpl implements DocumentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationServiceImpl.class);
    private final OcrService ocrService;

    @Override
    public boolean validateDocuments(MasterListRecord record, Map<DocumentType, MultipartFile> documents) {
        for (Map.Entry<DocumentType, MultipartFile> entry : documents.entrySet()) {
            DocumentType docType = entry.getKey();
            MultipartFile file = entry.getValue();

            try {
                // Step 1: Extract text from the document
                String extractedText = ocrService.getTextFromFile(file);
                if (extractedText == null || extractedText.isBlank()) {
                    logger.warn("OCR returned no text for document type {} for record {}", docType, record.getId());
                    return false;
                }
                
                String ocrTextLower = extractedText.toLowerCase();

                // --- NEW: Rule 1 - Check for the employee's full name ---
                boolean nameFound = ocrTextLower.contains(record.getFullName().toLowerCase());
                if (!nameFound) {
                    logger.warn("Validation FAILED for document type {} for record {}: Employee name '{}' not found in document.",
                            docType, record.getId(), record.getFullName());
                    return false; // Fail fast
                }

                // --- NEW: Rule 2 - Check for the presence of at least one required keyword ---
                Set<String> requiredKeywords = getRequiredKeywords(docType);
                boolean keywordFound = requiredKeywords.stream()
                        .anyMatch(keyword -> ocrTextLower.contains(keyword));

                if (!keywordFound) {
                    logger.warn("Validation FAILED for document type {} for record {}: No required keywords were found.",
                            docType, record.getId());
                    return false; // Fail fast
                }
                
            } catch (Exception e) {
                logger.error("Error during OCR processing for record {}", record.getId(), e);
                return false;
            }
        }

        logger.info("All documents successfully validated for record {}", record.getId());
        return true; // All documents passed validation
    }
    
    /**
     * NEW: Helper method to define required keywords for each document type.
     * The validation will pass if AT LEAST ONE of these keywords is found.
     */
    private Set<String> getRequiredKeywords(DocumentType documentType) {
        if (documentType == DocumentType.LETTER_OF_EMPLOYMENT) {
            return Set.of("employment", "offer", "appointment");
        }
        if (documentType == DocumentType.WORK_ID) {
            return Set.of("identity", "id", "card", "staff");
        }
        return Collections.emptySet();
    }
}