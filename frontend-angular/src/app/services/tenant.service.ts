import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, Subject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Tenant, UpdateTenantRequest } from '../models/tenant.model';
import { MasterListRecord } from '../models/master-list-record.model';

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private globalAdminApiUrl = 'http://localhost:8080/api/admin/tenants';
  private tenantAdminApiUrl = 'http://localhost:8080/api/v1/tenant-admin';
  private v1ApiUrl = 'http://localhost:8080/api/v1';
  
  private tenantsSubject = new BehaviorSubject<Tenant[]>([]);
  public tenants$ = this.tenantsSubject.asObservable();

  private recordsFilterSubject = new BehaviorSubject<string[] | null>(null);
  public recordsFilter$ = this.recordsFilterSubject.asObservable();

  constructor(private http: HttpClient) {}

  applyRecordFilter(recordIds: string[] | null): void {
    this.recordsFilterSubject.next(recordIds);
  }
  
  notifyForReverification(recordIds: string[]): Observable<any> {
    return this.http.post(`${this.v1ApiUrl}/records/notify`, { recordIds }).pipe(
      catchError(this.handleError)
    );
  }
  
  // Method for TenantAdmins to create users
  createUser(userData: any): Observable<any> {
    return this.http.post<any>(`${this.tenantAdminApiUrl}/users`, userData).pipe(
        catchError(this.handleError)
    );
  }
  
  // --- NEW METHOD for TenantAdmins to get users ---
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.tenantAdminApiUrl}/users`).pipe(
      catchError(this.handleError)
    );
  }

  // --- Methods for GLOBAL ADMINS ---
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
  
  // --- Methods for general use ---
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