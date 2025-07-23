import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, Subject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Tenant, UpdateTenantRequest } from '../models/tenant.model';
import { MasterListRecord } from '../models/master-list-record.model';
import { User } from '../models/user.model';
import { RecordStatus } from '../models/record-status.enum';
import { Ministry } from '../models/ministry.model';
import { Department } from '../models/department.model';

export interface BulkVerificationJob {
  id: string;
  status: string;
  statusMessage: string;
  totalRecords: number;
  processedRecords: number;
  successfullyVerifiedRecords: number;
  failedRecords: number;
  initiatedByEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface PayrollExportLog {
  id: string;
  exportTimestamp: string;
  recordCount: number;
  exportedByEmail: string;
  status: string;
  statusMessage: string;
}

export interface DashboardStats {
  totalUniqueRecords: number;
  totalVerified: number;
  totalValidated: number;
  totalPendingApproval: number;
  totalMismatched: number;
  totalNotFound: number;
  totalAwaitingReVerification: number;
  totalReviewers: number;
  totalSelfServiceUsers: number;
  totalAgentAccounts: number;
}

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private globalAdminApiUrl = 'http://localhost:8080/api/v1/global-admin/tenants';
  private v1ApiUrl = 'http://localhost:8080/api/v1';
  private tenantAdminApiUrl = 'http://localhost:8080/api/v1/tenant-admin';
  
  private tenantsSubject = new BehaviorSubject<Tenant[]>([]);
  public tenants$ = this.tenantsSubject.asObservable();

  private recordsFilterSubject = new BehaviorSubject<string[] | null>(null);
  public recordsFilter$ = this.recordsFilterSubject.asObservable();

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.v1ApiUrl}/dashboard/stats`).pipe(
      catchError(this.handleError)
    );
  }

  startBulkVerification(): Observable<any> {
    return this.http.post<any>(`${this.v1ApiUrl}/records/bulk-verify`, {}).pipe(
      catchError(this.handleError)
    );
  }

  getBulkJobHistory(): Observable<BulkVerificationJob[]> {
    return this.http.get<BulkVerificationJob[]>(`${this.v1ApiUrl}/bulk-jobs`).pipe(
      catchError(this.handleError)
    );
  }

  applyRecordFilter(recordIds: string[] | null): void {
    this.recordsFilterSubject.next(recordIds);
  }
  
  notifyForReverification(recordIds: string[]): Observable<any> {
    return this.http.post(`${this.v1ApiUrl}/records/notify`, { recordIds }).pipe(
      catchError(this.handleError)
    );
  }
  
  createUser(userData: Partial<User>): Observable<User> {
    return this.http.post<User>(`${this.tenantAdminApiUrl}/users`, userData).pipe(
        catchError(this.handleError)
    );
  }
  
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.tenantAdminApiUrl}/users`).pipe(
      catchError(this.handleError)
    );
  }

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.globalAdminApiUrl).pipe(
      tap(tenants => this.tenantsSubject.next(tenants)),
      catchError(this.handleError)
    );
  }

  createTenant(tenantData: Partial<Tenant>): Observable<Tenant> {
    return this.http.post<Tenant>(this.globalAdminApiUrl, tenantData).pipe(
      tap(newTenant => {
        const currentTenants = this.tenantsSubject.value;
        this.tenantsSubject.next([...currentTenants, newTenant]);
      }),
      catchError(this.handleError)
    );
  }

  updateTenant(id: string, tenantData: UpdateTenantRequest): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.globalAdminApiUrl}/${id}`, tenantData).pipe(
      tap(updatedTenant => {
        const currentTenants = this.tenantsSubject.value;
        const index = currentTenants.findIndex(t => t.id === id);
        if (index !== -1) {
          currentTenants[index] = updatedTenant;
          this.tenantsSubject.next([...currentTenants]);
        }
      }),
      catchError(this.handleError)
    );
  }

  deleteTenant(id: string): Observable<void> {
    return this.http.delete<void>(`${this.globalAdminApiUrl}/${id}`).pipe(
      tap(() => {
        const currentTenants = this.tenantsSubject.value;
        const filteredTenants = currentTenants.filter(t => t.id !== id);
        this.tenantsSubject.next(filteredTenants);
      }),
      catchError(this.handleError)
    );
  }

  checkSubdomainAvailability(subdomain: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.globalAdminApiUrl}/check-subdomain/${subdomain}`).pipe(
      catchError(this.handleError)
    );
  }
  
  // --- RESTORED THIS METHOD ---
  getRecordsForTenant(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records`).pipe(
      catchError(this.handleError)
    );
  }

  getFlaggedNotInSot(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records/flagged/not-in-sot`).pipe(
      catchError(this.handleError)
    );
  }

  uploadMasterList(file: File): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);
    const req = new HttpRequest('POST', `${this.v1ApiUrl}/records/upload`, formData, {
      reportProgress: true,
      responseType: 'json'
    });
    return this.http.request(req);
  }

  getPendingApprovalQueue(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records/queue/pending-approval`).pipe(
      catchError(this.handleError)
    );
  }

  getMismatchedQueue(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records/queue/mismatched`).pipe(
      catchError(this.handleError)
    );
  }

  updateRecord(recordId: string, recordData: Partial<MasterListRecord>): Observable<MasterListRecord> {
    return this.http.put<MasterListRecord>(`${this.v1ApiUrl}/records/${recordId}`, recordData).pipe(
      catchError(this.handleError)
    );
  }

  validateRecord(recordId: string, decision: 'VALIDATED' | 'REJECTED', comments: string): Observable<MasterListRecord> {
    const payload = { decision, comments };
    return this.http.post<MasterListRecord>(`${this.v1ApiUrl}/records/${recordId}/validate`, payload).pipe(
      catchError(this.handleError)
    );
  }

  resolveMismatch(recordId: string): Observable<MasterListRecord> {
    return this.http.post<MasterListRecord>(`${this.v1ApiUrl}/records/${recordId}/resolve-mismatch`, {}).pipe(
      catchError(this.handleError)
    );
  }

  getMinistries(): Observable<Ministry[]> {
    return this.http.get<Ministry[]>(`${this.tenantAdminApiUrl}/ministries`).pipe(
      catchError(this.handleError)
    );
  }

  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.tenantAdminApiUrl}/departments`).pipe(
      catchError(this.handleError)
    );
  }

  initiateExport(): Observable<any> {
    return this.http.post(`${this.v1ApiUrl}/records/export`, {}).pipe(
      catchError(this.handleError)
    );
  }

  getExportHistory(): Observable<PayrollExportLog[]> {
    return this.http.get<PayrollExportLog[]>(`${this.v1ApiUrl}/records/export-logs`).pipe(
      catchError(this.handleError)
    );
  }

  downloadExportFile(logId: string): Observable<Blob> {
    return this.http.get(`${this.v1ApiUrl}/records/export-logs/${logId}/download`, {
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }
  
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Error: ${error.error.message}`;
    } else {
      errorMessage = error.error?.message || `Error Code: ${error.status}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}