package com.proximaforte.bioverify.domain.enums;

/**
 * Enum representing the various states of an employee record throughout the verification workflow.
 * 
 * The typical flow is:
 * PENDING_VERIFICATION → AWAITING_REVIEW → REVIEWED → ACTIVE
 * 
 * Records can also be flagged for issues or become inactive due to missed checks.
 */
public enum RecordStatus {
    /** Initial state after CSV upload, awaiting external verification */
    PENDING_VERIFICATION,

    /** Successfully verified against external systems, awaiting human review */
    AWAITING_REVIEW,

    /** Human-reviewed and approved, ready for Proof of Life processing */
    REVIEWED,

    /** Fully verified and active, eligible for payroll */
    ACTIVE,

    /** Account suspended due to missed liveness checks */
    INACTIVE,

    /** Data discrepancy found between upload and Source of Truth */
    FLAGGED_DATA_MISMATCH,
    
    /** Employee not found in external Source of Truth system */
    FLAGGED_NOT_IN_SOT,
    
    /** Documents failed automated validation, require manual review */
    FLAGGED_INVALID_DOCUMENT,

    /** Record manually rejected by reviewer */
    REJECTED
}