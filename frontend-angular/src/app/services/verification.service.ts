import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Define interfaces for the verification flow
export interface VerificationRequest {
  ssid: string;
  nin: string;
}

// Updated to match the backend's VerificationResultDto
export interface VerificationResponse {
  recordId: string;
  newStatus: string; // e.g., 'PENDING_GRADE_VALIDATION', 'FLAGGED_DATA_MISMATCH'
  message: string;
}

// Interface for agent onboarding
export interface OnboardRequest {
  ssid: string;
  nin: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class VerificationService {
  // Define a new base URL for the records API
  private recordsApiUrl = 'http://localhost:8080/api/v1/records';
  private agentApiUrl = 'http://localhost:8080/api/v1/agent';

  constructor(private http: HttpClient) { }

  // --- Methods for Self-Service Users ---

  /**
   * Submits SSID and NIN to the new verification endpoint for a specific record.
   * @param recordId The ID of the master list record to verify.
   * @param data The verification data containing SSID and NIN.
   */
  verifyIdentity(recordId: string, data: VerificationRequest): Observable<VerificationResponse> {
    // Calls the new, more RESTful endpoint: POST /api/v1/records/{recordId}/verify
    return this.http.post<VerificationResponse>(`${this.recordsApiUrl}/${recordId}/verify`, data);
  }

  // The confirmVerification method has been removed as it's obsolete in the new workflow.

  // --- Methods for Enumerators (Unchanged) ---

  /**
   * Submits the full onboarding and verification package for an employee.
   */
  onboardUserByAgent(onboardRequest: OnboardRequest): Observable<any> {
    return this.http.post<any>(`${this.agentApiUrl}/onboard-user`, onboardRequest);
  }

  /**
   * Submits a liveness check for a given record.
   */
  performLivenessCheck(recordId: string): Observable<any> {
    return this.http.post<any>(`${this.agentApiUrl}/liveness-check`, { recordId });
  }
}