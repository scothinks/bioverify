// FILE: src/app/services/verification.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Define interfaces for the verification flow
export interface VerificationRequest {
  ssid: string;
  nin: string;
}

export interface VerificationResponse {
  success: boolean;
  message: string;
  record?: any; // The user's record details
}

@Injectable({
  providedIn: 'root'
})
export class VerificationService {
  private apiUrl = 'http://localhost:8080/api/v1/verification';

  constructor(private http: HttpClient) { }

  /**
   * Sends the user's SSID and NIN to the backend for verification.
   * @param data The verification request data.
   * @returns An Observable of the verification response.
   */
  verifyIdentity(data: VerificationRequest): Observable<VerificationResponse> {
    return this.http.post<VerificationResponse>(`${this.apiUrl}/verify`, data);
  }
}
