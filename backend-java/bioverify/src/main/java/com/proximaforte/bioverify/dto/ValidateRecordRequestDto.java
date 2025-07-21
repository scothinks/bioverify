package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.RecordStatus;
import lombok.Data;

@Data
public class ValidateRecordRequestDto {
    // Must be either VALIDATED or REJECTED
    private RecordStatus decision;
    private String comments;
}