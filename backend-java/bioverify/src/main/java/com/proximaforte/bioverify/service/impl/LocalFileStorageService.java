package com.proximaforte.bioverify.service.impl;

import com.proximaforte.bioverify.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.upload-dir:./storage/uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() throws IOException {
        // --- CORRECTED THIS LINE ---
        // Added .normalize() to clean up the path (e.g., remove './')
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        log.info("Initializing local file storage at: {}", this.rootLocation);
        Files.createDirectories(rootLocation);
    }

    @Override
    public String save(byte[] content, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank() || fileName.contains("..")) {
            throw new IOException("File name is invalid or contains path traversal characters.");
        }
        
        Path destinationFile = this.rootLocation.resolve(fileName).normalize();

        log.debug("Root storage path: {}", this.rootLocation);
        log.debug("Attempting to save to destination path: {}", destinationFile);
        
        if (!destinationFile.startsWith(this.rootLocation)) {
            log.error("SECURITY CHECK FAILED: Destination is not within the root storage directory.");
            log.error("Root: [{}], Destination: [{}]", this.rootLocation, destinationFile);
            throw new IOException("Cannot store file outside current directory.");
        }

        Files.write(destinationFile, content);
        log.info("Saved file to: {}", destinationFile);

        return fileName;
    }

    @Override
    public byte[] load(String fileIdentifier) throws IOException {
        if (fileIdentifier == null || fileIdentifier.isBlank()) {
            throw new IllegalArgumentException("File identifier cannot be empty.");
        }
        Path filePath = rootLocation.resolve(fileIdentifier).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new IOException("Failed to read file: " + fileIdentifier);
        }
        log.info("Loading file from: {}", filePath);
        return Files.readAllBytes(filePath);
    }
}