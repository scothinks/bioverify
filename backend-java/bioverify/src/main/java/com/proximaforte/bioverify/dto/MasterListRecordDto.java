package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
public class MasterListRecordDto {
    private UUID id;
    private String employeeId; // WID
    private String fullName;
    private String psn;
    private String ssid;
    private String nin;
    private String department;
    private String ministry;
    private String gradeLevel;
    private String salaryStructure;
    private RecordStatus status;
    private Boolean biometricStatus;
    private Instant validatedAt;
    private String validatedByEmail;
    private Instant createdAt;
    private String bvn;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String email;
    private String photoUrl;
    private List<String> documentUrls;
    
    // --- NEWLY ADDED FIELDS ---
    private LocalDate lastLivenessCheckDate;
    private LocalDate nextLivenessCheckDate;


    public MasterListRecordDto(MasterListRecord record) {
        this.id = record.getId();
        this.employeeId = record.getWid(); // CORRECTED: Mapped from wid instead of employeeId
        this.fullName = record.getFullName();
        this.psn = record.getPsn();
        this.ssid = record.getSsid();
        this.nin = record.getNin();
        this.department = record.getDepartment() != null ? record.getDepartment().getName() : null;
        this.ministry = record.getMinistry() != null ? record.getMinistry().getName() : null;
        this.gradeLevel = record.getGradeLevel();
        this.salaryStructure = record.getSalaryStructure();
        this.status = record.getStatus();
        this.biometricStatus = record.getBiometricStatus();
        this.validatedAt = record.getValidatedAt();
        this.validatedByEmail = record.getValidatedBy() != null ? record.getValidatedBy().getEmail() : null;
        this.createdAt = record.getCreatedAt();
        this.bvn = record.getBvn();
        this.dateOfBirth = record.getDateOfBirth();
        this.gender = record.getGender();
        this.phoneNumber = record.getPhoneNumber();
        this.email = record.getEmail();
        this.photoUrl = record.getPhotoUrl();
        this.documentUrls = record.getDocumentUrls();
        
        // --- MAPPINGS FOR NEW FIELDS ---
        this.lastLivenessCheckDate = record.getLastLivenessCheckDate();
        this.nextLivenessCheckDate = record.getNextLivenessCheckDate();
    }
}