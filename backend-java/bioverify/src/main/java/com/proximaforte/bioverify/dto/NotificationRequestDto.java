// FILE: src/main/java/com/proximaforte/bioverify/dto/NotificationRequestDto.java
package com.proximaforte.bioverify.dto;

import java.util.List;
import java.util.UUID;

public class NotificationRequestDto {
    private List<UUID> recordIds;
    public List<UUID> getRecordIds() { return recordIds; }
    public void setRecordIds(List<UUID> recordIds) { this.recordIds = recordIds; }
}