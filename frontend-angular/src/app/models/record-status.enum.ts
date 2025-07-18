// FILE: src/app/models/record-status.enum.ts

export enum RecordStatus {
  PendingVerification = 'PENDING_VERIFICATION',
  AwaitingRevalidation = 'AWAITING_REVALIDATION',
  PendingGradeValidation = 'PENDING_GRADE_VALIDATION',
  Validated = 'VALIDATED',
  FlaggedDataMismatch = 'FLAGGED_DATA_MISMATCH',
  FlaggedNotInSot = 'FLAGGED_NOT_IN_SOT',
  Rejected = 'REJECTED'
}