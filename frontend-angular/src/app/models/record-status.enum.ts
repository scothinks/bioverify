export enum RecordStatus {
  // Initial state after upload
  PendingVerification = 'PENDING_VERIFICATION',

  // Verified and awaiting human review
  AwaitingReview = 'AWAITING_REVIEW',

  // Reviewed by a human and ready for PoL
  Reviewed = 'REVIEWED',

  // Final "good" state after successful PoL
  Active = 'ACTIVE',

  // For missed liveness checks
  Inactive = 'INACTIVE',

  // Flagged states for various issues
  FlaggedDataMismatch = 'FLAGGED_DATA_MISMATCH',
  FlaggedNotInSot = 'FLAGGED_NOT_IN_SOT',
  FlaggedInvalidDocument = 'FLAGGED_INVALID_DOCUMENT',

  // Final state if a reviewer rejects the record
  Rejected = 'REJECTED'
}