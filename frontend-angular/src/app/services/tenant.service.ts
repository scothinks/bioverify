import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, Subject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Tenant, UpdateTenantRequest } from '../models/tenant.model';
import { MasterListRecord } from '../models/master-list-record.model';
import { BulkJob } from '../models/bulk-job.model';
import { User } from '../models/user.model';
// The import for RecordStatus is no longer needed in this specific method, but we'll leave it for now.
import { RecordStatus } from '../models/record-status.enum'; 

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private globalAdminApiUrl = 'http://localhost:8080/api/v1/global-admin/tenants';
  private v1ApiUrl = 'http://localhost:8080/api/v1';
  
  private tenantsSubject = new BehaviorSubject<Tenant[]>([]);
  public tenants$ = this.tenantsSubject.asObservable();

  private recordsFilterSubject = new BehaviorSubject<string[] | null>(null);
  public recordsFilter$ = this.recordsFilterSubject.asObservable();

  constructor(private http: HttpClient) {}

  startBulkVerification(): Observable<any> {
    return this.http.post<any>(`${this.v1ApiUrl}/records/bulk-verify`, {}).pipe(
      catchError(this.handleError)
    );
  }

  getBulkJobs(): Observable<BulkJob[]> {
    return this.http.get<BulkJob[]>(`${this.v1ApiUrl}/bulk-jobs`).pipe(
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
    return this.http.post<User>(`${this.v1ApiUrl}/users`, userData).pipe(
        catchError(this.handleError)
    );
  }
  
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.v1ApiUrl}/users`).pipe(
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
  
  getRecordsForTenant(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records`).pipe(
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

  // --- NEW METHODS FOR VALIDATION WORKFLOW ---

  getValidationQueue(): Observable<MasterListRecord[]> {
    return this.http.get<MasterListRecord[]>(`${this.v1ApiUrl}/records/validation-queue`).pipe(
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