// FILE: src/main/java/com/proximaforte/bioverify/dto/UploadSummaryDto.java
package com.proximaforte.bioverify.dto;

import java.util.List;
import java.util.UUID;

public class UploadSummaryDto {

    private int newRecordsCreated;
    private int existingRecordsUpdated;
    private List<UUID> recordsRequiringReverificationIds;

    public UploadSummaryDto(int newRecordsCreated, int existingRecordsUpdated, List<UUID> recordsRequiringReverificationIds) {
        this.newRecordsCreated = newRecordsCreated;
        this.existingRecordsUpdated = existingRecordsUpdated;
        this.recordsRequiringReverificationIds = recordsRequiringReverificationIds;
    }

    // Getters
    public int getNewRecordsCreated() { return newRecordsCreated; }
    public int getExistingRecordsUpdated() { return existingRecordsUpdated; }
    public List<UUID> getRecordsRequiringReverificationIds() { return recordsRequiringReverificationIds; }
}