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
    private String department; // <-- RENAMED from businessUnit
    private String ministry; // <-- ADDED
    private String gradeLevel;
    private String salaryStructure; // <-- ADDED
    private RecordStatus status;
    private Boolean biometricStatus;
    private Instant createdAt;

    // Convenience constructor that maps an entity to this DTO
    public MasterListRecordDto(MasterListRecord record) {
        this.id = record.getId();
        this.fullName = record.getFullName();
        this.department = record.getDepartment(); 
        this.ministry = record.getMinistry(); 
        this.gradeLevel = record.getGradeLevel();
        this.salaryStructure = record.getSalaryStructure();
        this.status = record.getStatus();
        this.biometricStatus = record.getBiometricStatus();
        this.createdAt = record.getCreatedAt();
    }
}