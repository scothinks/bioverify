package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * A mock implementation of the OcrService for local development and testing.
 * It does not perform real OCR. Instead, it returns a dummy string containing the filename.
 * This allows the DocumentValidationService to be tested without a live OCR provider.
 */
@Service
@Profile("local") // This service will only be active when the Spring profile is "local"
public class MockOcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(MockOcrServiceImpl.class);

    @Override
    public String getTextFromFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }
        
        String mockText = "Mock OCR text for file: " + file.getOriginalFilename();
        logger.info("Using MockOcrService. Returning dummy text: \"{}\"", mockText);
        
        return mockText;
    }
}