package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.dto.NotificationRequestDto;
import com.proximaforte.bioverify.dto.UploadSummaryDto;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.UserRepository;
import com.proximaforte.bioverify.service.MasterListUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MasterListUploadController {

    private final MasterListUploadService uploadService;
    private final UserRepository userRepository;
    private final MasterListRecordRepository recordRepository;

    @Autowired
    public MasterListUploadController(
            MasterListUploadService uploadService,
            UserRepository userRepository,
            MasterListRecordRepository recordRepository) {
        this.uploadService = uploadService;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
    }

    @GetMapping("/records")
    public ResponseEntity<List<MasterListRecordDto>> getTenantRecords(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
        
        Tenant tenant = user.getTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of());
        }

        List<MasterListRecord> records = recordRepository.findAllByTenantId(tenant.getId());

        List<MasterListRecordDto> recordDtos = records.stream()
                .map(record -> new MasterListRecordDto(
                        record.getId(),
                        record.getFullName(),
                        record.getBusinessUnit(),
                        record.getGradeLevel(),
                        record.getStatus(),
                        record.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(recordDtos);
    }
    
    @PostMapping("/records/upload")
    public ResponseEntity<?> uploadMasterList(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in the database."));
        
        Tenant tenant = user.getTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error: User is not associated with a tenant."));
        }

        try {
            UploadSummaryDto summary = uploadService.processUpload(file, tenant.getId());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to process file: " + e.getMessage()));
        }
    }

    @PostMapping("/records/notify")
    public ResponseEntity<?> notifyForReverification(@RequestBody NotificationRequestDto request) {
        System.out.println("Received request to notify users for record IDs: " + request.getRecordIds());
        return ResponseEntity.ok(Map.of("message", "Notification process initiated for " + request.getRecordIds().size() + " users."));
    }
}