package com.proximaforte.bioverify.domain.enums;

public enum RecordStatus {
    PENDING_VERIFICATION,
    AWAITING_REVALIDATION,
    PENDING_GRADE_VALIDATION,
    VALIDATED,
    FLAGGED_DATA_MISMATCH,
    FLAGGED_NOT_IN_SOT,
    REJECTED
}