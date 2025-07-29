package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.DocumentType;
import com.proximaforte.bioverify.service.DocumentValidationService;
import com.proximaforte.bioverify.service.OcrService; // Assumed interface for an OCR provider
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentValidationServiceImpl implements DocumentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationServiceImpl.class);
    private final OcrService ocrService; // Prerequisite: An implementation for an OCR service.

    @Override
    public boolean validateDocuments(MasterListRecord record, Map<DocumentType, MultipartFile> documents) {
        for (Map.Entry<DocumentType, MultipartFile> entry : documents.entrySet()) {
            DocumentType docType = entry.getKey();
            MultipartFile file = entry.getValue();

            try {
                // Step 1: Extract text from the document using the OCR service
                String extractedText = ocrService.getTextFromFile(file);
                if (extractedText == null || extractedText.isBlank()) {
                    logger.warn("OCR returned no text for document type {} for record {}", docType, record.getId());
                    return false;
                }
                
                // Step 2: Apply validation rules based on document type
                boolean isValid = switch (docType) {
                    case LETTER_OF_EMPLOYMENT, WORK_ID ->
                        // Simplified Rule: Check if the full name is present in the document
                        extractedText.toLowerCase().contains(record.getFullName().toLowerCase());
                    default ->
                        // By default, unknown document types are considered invalid
                        false;
                };
                
                if (!isValid) {
                    logger.warn("Validation FAILED for document type {} for record {}", docType, record.getId());
                    return false; // If any document fails, the whole process fails
                }
                
            } catch (Exception e) {
                logger.error("Error during OCR processing for record {}", record.getId(), e);
                return false;
            }
        }

        logger.info("All documents successfully validated for record {}", record.getId());
        return true; // All documents passed validation
    }
}