export interface BulkJob {
  id: string;
  uploadId: string | null;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  statusMessage: string | null;
  totalRecords: number;
  processedRecords: number;
  successfullyVerifiedRecords: number;
  failedRecords: number;
  createdAt: string;
  updatedAt: string;
}