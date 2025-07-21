package com.proximaforte.bioverify.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateRecordRequestDto {
    private String fullName;
    private String department;
    private String ministry; 
    private String gradeLevel;
    private String salaryStructure;
    private LocalDate dateOfBirth;
}