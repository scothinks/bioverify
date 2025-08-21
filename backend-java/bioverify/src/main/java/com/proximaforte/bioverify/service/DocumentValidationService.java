package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface DocumentValidationService {

    /**
     * Validates the content of uploaded documents against a given record.
     *
     * @param record The MasterListRecord to validate against.
     * @param documents A map where the key is the document type and the value is the file.
     * @return true if all documents are valid, false otherwise.
     */
    boolean validateDocuments(MasterListRecord record, Map<DocumentType, MultipartFile> documents);
}