// FILE: src/app/models/master-list-record.model.ts

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
  dateOfBirth?: string; // Or Date

  // Timestamps and audit fields
  createdAt: string; 
  updatedAt: string;
  verifiedAt?: string;
  validatedAt?: string;
  lastProofOfLifeDate?: string;
}