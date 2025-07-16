import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Define interfaces for the verification flow
export interface VerificationRequest {
  ssid: string;
  nin: string;
}

export interface OnboardRequest {
  ssid: string;
  nin: string;
  email: string;
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
  private verificationApiUrl = 'http://localhost:8080/api/v1/verification';
  private agentApiUrl = 'http://localhost:8080/api/v1/agent';

  constructor(private http: HttpClient) { }

  // --- Methods for Self-Service Users ---

  verifyIdentity(data: VerificationRequest): Observable<VerificationResponse> {
    return this.http.post<VerificationResponse>(`${this.verificationApiUrl}/verify`, data);
  }

  confirmVerification(recordId: string): Observable<any> {
    return this.http.post<any>(`${this.verificationApiUrl}/confirm`, { recordId });
  }

  // --- Methods for Enumerators ---

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