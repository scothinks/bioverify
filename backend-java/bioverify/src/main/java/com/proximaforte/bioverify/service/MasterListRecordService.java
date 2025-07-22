package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Department;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Ministry;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.UpdateRecordRequestDto;
import com.proximaforte.bioverify.dto.ValidateRecordRequestDto;
import com.proximaforte.bioverify.repository.DepartmentRepository;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.MinistryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MasterListRecordService {

    private final MasterListRecordRepository recordRepository;
    private final DepartmentRepository departmentRepository;
    private final MinistryRepository ministryRepository;
    private final EmployeeIdService employeeIdService; // Injected new service

    /**
     * Fetches records for human review. If the user is a REVIEWER, it filters
     * records based on their assigned Ministries and Departments. If the user is
     * a TENANT_ADMIN, it returns all records for the tenant.
     */
    public List<MasterListRecord> getValidationQueue(User currentUser) {
        UUID tenantId = currentUser.getTenant().getId();
        List<RecordStatus> statuses = List.of(RecordStatus.PENDING_GRADE_VALIDATION, RecordStatus.FLAGGED_DATA_MISMATCH);

        // Tenant Admins see everything.
        if (currentUser.getRole() == Role.TENANT_ADMIN) {
            return recordRepository.findByTenantIdAndStatusIn(tenantId, statuses);
        }

        // Reviewers only see records from their assigned areas.
        if (currentUser.getRole() == Role.REVIEWER) {
            Set<Department> depts = currentUser.getAssignedDepartments();
            Set<Ministry> mins = currentUser.getAssignedMinistries();

            // If a reviewer has no assignments, they see nothing.
            if ((depts == null || depts.isEmpty()) && (mins == null || mins.isEmpty())) {
                return Collections.emptyList();
            }

            // Call the new custom query method
            return recordRepository.findRecordsByReviewerAssignments(tenantId, statuses, depts, mins);
        }

        // Other roles see nothing by default.
        return Collections.emptyList();
    }

    /**
     * Allows a reviewer to update certain fields of a record.
     */
    @Transactional
    public MasterListRecord updateRecordData(UUID recordId, UpdateRecordRequestDto request, User reviewer) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Record not found with ID: " + recordId));

        if (request.getFullName() != null) record.setFullName(request.getFullName());
        if (request.getGradeLevel() != null) record.setGradeLevel(request.getGradeLevel());
        if (request.getSalaryStructure() != null) record.setSalaryStructure(request.getSalaryStructure());
        if (request.getDateOfBirth() != null) record.setDateOfBirth(request.getDateOfBirth());

        Tenant tenant = record.getTenant();
        if (request.getDepartment() != null) {
            record.setDepartment(findOrCreateDepartment(request.getDepartment(), tenant));
        }
        if (request.getMinistry() != null) {
            record.setMinistry(findOrCreateMinistry(request.getMinistry(), tenant));
        }

        record.setLastUpdatedBy(reviewer);
        return recordRepository.save(record);
    }

    /**
     * Sets the final status of a record to VALIDATED or REJECTED.
     * If validated, it also generates the employee's Work ID (WID).
     */
    @Transactional
    public MasterListRecord performValidation(UUID recordId, ValidateRecordRequestDto request, User reviewer) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Record not found with ID: " + recordId));

        RecordStatus decision = request.getDecision();
        if (decision != RecordStatus.VALIDATED && decision != RecordStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be either VALIDATED or REJECTED.");
        }

        // If the decision is to validate and the record doesn't already have a Work ID, generate one.
        if (decision == RecordStatus.VALIDATED && record.getEmployeeId() == null) {
            String newWorkId = employeeIdService.generateNewWorkId(record.getTenant());
            record.setEmployeeId(newWorkId);
        }

        record.setStatus(decision);
        record.setValidatedBy(reviewer);
        record.setValidatedAt(Instant.now());

        return recordRepository.save(record);
    }

    @Transactional
    public MasterListRecord updatePsnForRecord(UUID recordId, String newPsn) {
        MasterListRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Record not found with ID: " + recordId));

        record.setPsn(newPsn);
        record.setPsnHash(toSha256(newPsn));

        return recordRepository.save(record);
    }

    private String toSha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find SHA-256 algorithm", e);
        }
    }

    private Department findOrCreateDepartment(String name, Tenant tenant) {
        if (name == null || name.isBlank()) return null;
        return departmentRepository.findByNameAndTenantId(name, tenant.getId())
                .orElseGet(() -> {
                    Department newDept = new Department();
                    newDept.setName(name);
                    newDept.setTenant(tenant);
                    return departmentRepository.save(newDept);
                });
    }

    private Ministry findOrCreateMinistry(String name, Tenant tenant) {
        if (name == null || name.isBlank()) return null;
        return ministryRepository.findByNameAndTenantId(name, tenant.getId())
                .orElseGet(() -> {
                    Ministry newMin = new Ministry();
                    newMin.setName(name);
                    newMin.setTenant(tenant);
                    return ministryRepository.save(newMin);
                });
    }
}