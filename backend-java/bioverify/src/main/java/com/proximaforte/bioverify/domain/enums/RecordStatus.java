package com.proximaforte.bioverify.domain.enums;

public enum RecordStatus {
    // Initial state after upload
    PENDING_VERIFICATION,

    // NEW: Verified and awaiting human review
    AWAITING_REVIEW,

    // NEW: Reviewed by a human and ready for PoL
    REVIEWED,

    // Final "good" state after successful PoL
    ACTIVE,

    // NEW: Replaces SUSPENDED for missed liveness checks
    INACTIVE,

    // Flagged states for various issues
    FLAGGED_DATA_MISMATCH,
    FLAGGED_NOT_IN_SOT,
    FLAGGED_INVALID_DOCUMENT,

    // Final state if a reviewer rejects the record
    REJECTED
}