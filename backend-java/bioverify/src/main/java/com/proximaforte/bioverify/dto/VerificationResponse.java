package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.MasterListRecord;

/**
 * Data Transfer Object for a verification response.
 * Contains the result of the verification check.
 */
public class VerificationResponse {
    private boolean success;
    private String message;
    private MasterListRecord record; // The user's record, if verification is successful

    public VerificationResponse(boolean success, String message, MasterListRecord record) {
        this.success = success;
        this.message = message;
        this.record = record;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public MasterListRecord getRecord() { return record; }
    public void setRecord(MasterListRecord record) { this.record = record; }
}
