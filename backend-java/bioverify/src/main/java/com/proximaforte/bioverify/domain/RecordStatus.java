// FILE: src/main/java/com/proximaforte/bioverify/domain/RecordStatus.java
package com.proximaforte.bioverify.domain;

public enum RecordStatus {
    UPLOADED,
    PENDING,
    VERIFIED,
    FAILED,
    ACTION_REQUIRED,
    VERIFIED_PENDING_CONFIRMATION, 
    FLAGGED_SSID_NIN_MISMATCH,
    AWAITING_REVERIFICATION;   
}