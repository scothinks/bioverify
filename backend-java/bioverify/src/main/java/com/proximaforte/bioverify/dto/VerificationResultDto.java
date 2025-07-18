package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.RecordStatus;
import lombok.Getter;
import java.util.UUID;

@Getter
public class VerificationResultDto {
    private final UUID recordId;
    private final RecordStatus newStatus;
    private final String message;

    public VerificationResultDto(UUID recordId, RecordStatus newStatus, String message) {
        this.recordId = recordId;
        this.newStatus = newStatus;
        this.message = message;
    }
}