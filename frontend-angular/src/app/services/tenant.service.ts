import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Tenant, UpdateTenantRequest } from '../models/tenant.model';

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private adminApiUrl = 'http://localhost:8080/api/admin/tenants';
  private v1ApiUrl = 'http://localhost:8080/api/v1'; // Base URL for v1 APIs
  private tenantsSubject = new BehaviorSubject<Tenant[]>([]);
  public tenants$ = this.tenantsSubject.asObservable();

  constructor(private http: HttpClient) {}

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.adminApiUrl).pipe(
      tap(tenants => this.tenantsSubject.next(tenants)),
      catchError(this.handleError)
    );
  }

  createTenant(tenantData: Partial<Tenant>): Observable<Tenant> {
    return this.http.post<Tenant>(this.adminApiUrl, tenantData).pipe(
      tap(newTenant => {
        const currentTenants = this.tenantsSubject.value;
        this.tenantsSubject.next([...currentTenants, newTenant]);
      }),
      catchError(this.handleError)
    );
  }

  updateTenant(id: string, tenantData: UpdateTenantRequest): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.adminApiUrl}/${id}`, tenantData).pipe(
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
    return this.http.delete<void>(`${this.adminApiUrl}/${id}`).pipe(
      tap(() => {
        const currentTenants = this.tenantsSubject.value;
        const filteredTenants = currentTenants.filter(t => t.id !== id);
        this.tenantsSubject.next(filteredTenants);
      }),
      catchError(this.handleError)
    );
  }

  checkSubdomainAvailability(subdomain: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.adminApiUrl}/check-subdomain/${subdomain}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * METHOD ADDED BACK: Uploads a master list file to the backend.
   * @param file The file to be uploaded.
   * @param tenantId The ID of the tenant this file belongs to.
   * @returns An Observable of the HTTP event, allowing for progress tracking.
   */
  uploadMasterList(file: File, tenantId: string): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', `${this.v1ApiUrl}/${tenantId}/records/upload`, formData, {
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