// FILE: src/main/java/com/proximaforte/bioverify/dto/MasterListRecordDto.java
package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.RecordStatus;
import java.time.Instant;
import java.util.UUID;

public class MasterListRecordDto {

    private UUID id;
    private String fullName;
    private String businessUnit;
    private String gradeLevel;
    private RecordStatus status;
    private Instant createdAt;

    // Constructor to map from the entity to the DTO
    public MasterListRecordDto(UUID id, String fullName, String businessUnit, String gradeLevel, RecordStatus status, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.businessUnit = businessUnit;
        this.gradeLevel = gradeLevel;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Standard Getters
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getBusinessUnit() { return businessUnit; }
    public String getGradeLevel() { return gradeLevel; }
    public RecordStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}