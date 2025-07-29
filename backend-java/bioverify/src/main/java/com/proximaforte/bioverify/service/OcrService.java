package com.proximaforte.bioverify.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface OcrService {

    /**
     * Extracts machine-readable text from a given file.
     *
     * @param file The file (e.g., an image or PDF) to process.
     * @return The text content extracted from the file.
     * @throws IOException if the file cannot be read.
     */
    String getTextFromFile(MultipartFile file) throws IOException;
}