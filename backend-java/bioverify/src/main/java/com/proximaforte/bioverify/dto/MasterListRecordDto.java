package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import lombok.Getter;
import java.time.Instant;
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

    public MasterListRecordDto(MasterListRecord record) {
        this.id = record.getId();
        this.employeeId = record.getEmployeeId();
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
    }
}