import { RecordStatus } from './record-status.enum';

export interface MasterListRecord {
  id: string;
  employeeId?: string;
  fullName: string;
  psn?: string;
  ssid?: string;
  nin?: string;
  department?: string;
  ministry?: string;
  gradeLevel?: string;
  salaryStructure?: string;
  status: RecordStatus;
  biometricStatus?: boolean;
  validatedAt?: string;
  validatedByEmail?: string;
  createdAt: string;

  
  dateOfBirth?: string;
  originalUploadData?: string;
  sotData?: string;
}