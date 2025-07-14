// FILE: src/app/models/master-list-record.model.ts

export interface MasterListRecord {
  id: string;
  fullName: string;
  businessUnit: string;
  gradeLevel: string;
  ssid?: string;
  nin?: string;
  lastProofOfLifeDate?: string;
  createdAt: string; 
  updatedAt: string; 
  
  
  status: 'PENDING' | 'VERIFIED' | 'FAILED' | 'ACTION_REQUIRED' | 'UPLOADED' | 'VERIFIED_PENDING_CONFIRMATION' | 'FLAGGED_SSID_NIN_MISMATCH' | 'AWAITING_REVERIFICATION';
}