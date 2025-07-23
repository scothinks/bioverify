import { RecordStatus } from './record-status.enum';

export interface MasterListRecord {
  id: string;
  fullName: string;
  department: string;
  ministry?: string;
  gradeLevel: string;
  salaryStructure?: string;
  status: RecordStatus;

  // Optional fields that may or may not be present
  ssid?: string;
  nin?: string;
  dateOfBirth?: string;

  // --- ADDED FOR MISMATCH RESOLUTION ---
  // Raw JSON strings for side-by-side comparison in the UI.
  originalUploadData?: string;
  sotData?: string;

  // Timestamps and audit fields
  createdAt: string;
  updatedAt: string;
  verifiedAt?: string;
  validatedAt?: string;
  lastProofOfLifeDate?: string;
}