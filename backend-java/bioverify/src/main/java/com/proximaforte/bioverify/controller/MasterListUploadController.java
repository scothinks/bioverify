package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.service.MasterListUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for handling master list file uploads.
 */
@RestController
@RequestMapping("/api/v1") // Base path for version 1 of our API
public class MasterListUploadController {

    private final MasterListUploadService uploadService;

    @Autowired
    public MasterListUploadController(MasterListUploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * API endpoint to upload a master list CSV file for a specific tenant.
     * Listens for HTTP POST requests to /api/v1/{tenantId}/records/upload.
     * The request must be a multipart/form-data request.
     *
     * @param tenantId The ID of the tenant, extracted from the URL path.
     * @param file The uploaded file, extracted from the request part named "file".
     * @return A ResponseEntity with a success message and the count of records created.
     */
    @PostMapping("/{tenantId}/records/upload")
    public ResponseEntity<?> uploadMasterList(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file) {

        // Basic validation to ensure a file was actually uploaded.
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please select a file to upload."));
        }

        try {
            int recordsSaved = uploadService.processUpload(file, tenantId);
            // Return a success response with the number of records processed.
            return ResponseEntity.ok(Map.of(
                "message", "File uploaded and processed successfully.",
                "recordsCreated", recordsSaved
            ));
        } catch (Exception e) {
            // In case of any error during processing, return an internal server error status.
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to process file: " + e.getMessage()
            ));
        }
    }
}
