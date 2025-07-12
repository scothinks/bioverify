// FILE: src/app/services/tenant.service.ts (MODIFIED)
// We are adding a new method to our existing service.

import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tenant } from '../models/tenant.model';

@Injectable({
  providedIn: 'root'
})
export class TenantService {

  private baseUrl = 'http://localhost:8080'; // Using a base URL for easier management

  constructor(private http: HttpClient) { }

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(`${this.baseUrl}/api/admin/tenants`);
  }
  uploadMasterList(file: File, tenantId: string): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', `${this.baseUrl}/api/v1/${tenantId}/records/upload`, formData, {
      reportProgress: true,
      responseType: 'json'
    });

    return this.http.request(req);
  }
}