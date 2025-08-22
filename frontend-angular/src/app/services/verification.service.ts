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
  private agentApiUrl = 'http://localhost:8080/api/v1/agent';

  constructor(private http: HttpClient) { }

  // --- Methods for Public Self-Service Flow ---

  initiatePublicVerification(data: VerifyIdentityRequest): Observable<InitiateVerificationResponse> {
    return this.http.post<InitiateVerificationResponse>(`${this.publicVerificationApiUrl}/initiate`, data);
  }

  resolvePublicPsnChallenge(data: PsnChallengeRequest): Observable<PsnChallengeResponse> {
    return this.http.post<PsnChallengeResponse>(`${this.publicVerificationApiUrl}/challenge`, data);
  }

  // Methods for Authenticated Agent Flow

  initiateAgentVerification(data: VerifyIdentityRequest): Observable<InitiateVerificationResponse> {
    // Calls the secure agent endpoint
    return this.http.post<InitiateVerificationResponse>(`${this.agentApiUrl}/verify`, data);
  }

  resolveAgentPsnChallenge(data: PsnChallengeRequest): Observable<PsnChallengeResponse> {
    // Calls the secure agent endpoint
    return this.http.post<PsnChallengeResponse>(`${this.agentApiUrl}/challenge`, data);
  }


  // --- Method for Authenticated Users (e.g., User Dashboard) ---

  reverifyByIdentity(recordId: string, data: VerifyIdentityRequest): Observable<VerificationResult> {
    return this.http.post<VerificationResult>(`${this.agentApiUrl}/records/${recordId}/verify`, data);
  }
}