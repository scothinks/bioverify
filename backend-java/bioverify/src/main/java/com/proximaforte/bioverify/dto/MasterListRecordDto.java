package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class MasterListRecordDto {
    private UUID id;
    private String fullName;
    private String businessUnit;
    private String gradeLevel;
    private RecordStatus status;
    private Boolean biometricStatus; // --- NEWLY ADDED FIELD ---
    private Instant createdAt;

    // Convenience constructor that maps an entity to this DTO
    public MasterListRecordDto(MasterListRecord record) {
        this.id = record.getId();
        this.fullName = record.getFullName();
        this.businessUnit = record.getBusinessUnit();
        this.gradeLevel = record.getGradeLevel();
        this.status = record.getStatus();
        this.biometricStatus = record.getBiometricStatus(); // --- MAP THE NEW FIELD ---
        this.createdAt = record.getCreatedAt();
    }
}