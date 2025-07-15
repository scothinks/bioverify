// FILE: src/main/java/com/proximaforte/bioverify/dto/ConfirmationRequestDto.java
package com.proximaforte.bioverify.dto;

import java.util.UUID;

public class ConfirmationRequestDto {
    private UUID recordId;

    public UUID getRecordId() {
        return recordId;
    }

    public void setRecordId(UUID recordId) {
        this.recordId = recordId;
    }
}