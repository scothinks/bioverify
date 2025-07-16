package com.proximaforte.bioverify.dto;

// No MasterListRecord import needed here anymore

/**
 * Data Transfer Object for a verification response.
 * Contains the result of the verification check.
 */
public class VerificationResponse {
    private boolean success;
    private String message;
    private MasterListRecordDto record; // <-- UPDATED: Changed from entity to DTO

    public VerificationResponse(boolean success, String message, MasterListRecordDto record) { 
        this.success = success;
        this.message = message;
        this.record = record;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public MasterListRecordDto getRecord() { return record; }

    // Setters are typically not needed for response DTOs but are kept for consistency
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setRecord(MasterListRecordDto record) { this.record = record; } 
}