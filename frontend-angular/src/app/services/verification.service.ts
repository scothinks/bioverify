// src/app/services/verification.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// --- Interfaces for Public Self-Service Flow ---

export interface VerifyIdentityRequest {
  ssid: string;
  nin: string;
}

export interface InitiateVerificationResponse {
  nextStep: 'CHALLENGE_PSN' | 'CREATE_ACCOUNT';
  recordId: string;
}

export interface PsnChallengeRequest {
  recordId: string;
  psn: string;
}

export interface PsnChallengeResponse {
  success: boolean;
  message: string;
}

// --- Interface for Authenticated Re-Verification Flow ---

export interface VerificationResult {
  recordId: string;
  newStatus: string;
  message: string;
}


@Injectable({
  providedIn: 'root'
})
export class VerificationService {
  private publicVerificationApiUrl = 'http://localhost:8080/api/v1/verification';
  // Use the agent endpoint for authenticated re-verification
  private agentApiUrl = 'http://localhost:8080/api/v1/agent';

  constructor(private http: HttpClient) { }

  // --- Methods for Public Self-Service Flow ---

  initiateVerification(data: VerifyIdentityRequest): Observable<InitiateVerificationResponse> {
    return this.http.post<InitiateVerificationResponse>(`${this.publicVerificationApiUrl}/initiate`, data);
  }

  resolvePsnChallenge(data: PsnChallengeRequest): Observable<PsnChallengeResponse> {
    return this.http.post<PsnChallengeResponse>(`${this.publicVerificationApiUrl}/challenge`, data);
  }

  // --- Method for Authenticated Users (e.g., User Dashboard) ---

  /**
   * For an authenticated user to re-verify their identity against a known record.
   * This calls the same kind of endpoint an Agent would use.
   */
  reverifyByIdentity(recordId: string, data: VerifyIdentityRequest): Observable<VerificationResult> {
    // This assumes the backend's AgentController has an endpoint like this.
    // We will fix this on the backend after the frontend is corrected.
    return this.http.post<VerificationResult>(`${this.agentApiUrl}/records/${recordId}/verify`, data);
  }
}