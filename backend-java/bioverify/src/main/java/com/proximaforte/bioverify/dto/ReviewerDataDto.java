package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.Department;
import com.proximaforte.bioverify.domain.Ministry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDataDto {
    private UUID id;
    private String fullName;
    private String email;
    private long pendingValidationCount;
    private Set<MinistryDto> assignedMinistries;
    private Set<DepartmentDto> assignedDepartments;
}