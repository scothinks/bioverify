/**
 * Authentication service handling JWT-based authentication and authorization.
 * 
 * This service manages the complete user authentication lifecycle including:
 * - Login/logout with JWT access and refresh tokens
 * - Token storage and automatic refresh mechanism
 * - Role-based routing and authorization
 * - Account creation and activation workflows
 * - Session management with reactive state updates
 * 
 * The service integrates with the backend authentication endpoints and provides
 * a centralized authentication state management for the entire application.
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { MasterListRecordDto } from './pol.service';

/**
 * JWT response structure matching backend JwtResponse DTO.
 * Contains both access and refresh tokens plus user metadata.
 */
export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  type: string;
  id: string;
  email: string;
  role: string;
}

/**
 * Flexible request interface for various authentication operations.
 * Used for login, registration, account creation, and identity verification.
 */
export interface AuthRequest {
  email?: string;
  password?: string;
  fullName?: string;
  role?: string;
  ssid?: string;
  nin?: string;
  recordId?: string;
}

/**
 * Decoded JWT token payload structure.
 * Contains user identity, role, and tenant information for authorization.
 */
export interface DecodedToken {
  sub: string;        // User ID (subject)
  role: string;       // User role for authorization
  tenantId?: string;  // Multi-tenant isolation
  status?: string;    // Account status
  iat: number;        // Issued at timestamp
  exp: number;        // Expiration timestamp
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // API endpoint configurations
  private authApiUrl = 'http://localhost:8080/api/v1/auth';
  private v1ApiUrl = 'http://localhost:8080/api/v1';

  // Local storage keys for token persistence
  private readonly ACCESS_TOKEN_KEY = 'bioverify_access_token';
  private readonly REFRESH_TOKEN_KEY = 'bioverify_refresh_token';

  // Reactive authentication state management
  private isLoggedInSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  /**
   * Authenticates user with email/password and manages token storage.
   * 
   * @param credentials User email and password for authentication
   * @returns Observable with JWT response containing access and refresh tokens
   */
  login(credentials: AuthRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.authApiUrl}/authenticate`, credentials).pipe(
      tap(response => {
        // Store both tokens in localStorage for persistent sessions
        this.setAccessToken(response.accessToken);
        this.setRefreshToken(response.refreshToken);
        // Update reactive authentication state
        this.isLoggedInSubject.next(true);
        // Navigate user to appropriate dashboard based on their role
        this.redirectUserBasedOnRole();
      }),
      catchError(this.handleError)
    );
  }

  // RESTORED: This method is vital for creating admin/reviewer users
  register(userData: AuthRequest): Observable<any> {
    return this.http.post<any>(`${this.authApiUrl}/register`, userData).pipe(
      catchError(this.handleError)
    );
  }

  // RESTORED: This method is vital for the manual account creation flow
  createAccount(accountData: AuthRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.authApiUrl}/create-account`, accountData).pipe(
      tap(response => {
        // NOTE: This flow may need review; it currently sets tokens upon account creation
        this.setAccessToken(response.accessToken);
        this.setRefreshToken(response.refreshToken);
      }),
      catchError(this.handleError)
    );
  }

  // NEW: Method to call the backend activation endpoint
  activateAccount(token: string, password: string): Observable<any> {
    return this.http.post(`${this.authApiUrl}/activate-account`, { token, password });
  }

  // NEW: Method to request a new activation link
  resendActivationLink(email: string): Observable<any> {
    return this.http.post(`${this.authApiUrl}/resend-activation`, { email });
  }

  // NEW: Method to call the refresh token endpoint
  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      return this.http.post<any>(`${this.authApiUrl}/refreshtoken`, { refreshToken }).pipe(
        tap((response) => {
          this.setAccessToken(response.accessToken);
        })
      );
    }
    return throwError(() => new Error('No refresh token available'));
  }

  // UPDATED: Calls the backend logout and clears all tokens from storage
  logout(): void {
    const refreshToken = this.getRefreshToken();
    // No need to wait for the HTTP call to complete for faster UI response
    if (refreshToken) {
        this.http.post(`${this.authApiUrl}/logout`, { refreshToken }).subscribe();
    }
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.isLoggedInSubject.next(false);
    this.router.navigate(['/login']);
  }

  getCurrentUserRecord(): Observable<MasterListRecordDto> {
    return this.http.get<MasterListRecordDto>(`${this.v1ApiUrl}/users/me/record`).pipe(
        catchError(this.handleError)
    );
  }

  // --- Token Helper Methods ---
  public getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  private setAccessToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  public getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  // --- Role, Status, and Decode Methods ---
  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    if (!token) {
        return false;
    }
    return !this.isTokenExpired(token);
  }

  redirectUserBasedOnRole(): void {
    const role = this.getUserRole();
    switch (role) {
      case 'GLOBAL_SUPER_ADMIN': this.router.navigate(['/dashboard/global-admin']); break;
      case 'TENANT_ADMIN': this.router.navigate(['/dashboard/tenant-admin']); break;
      case 'AGENT': this.router.navigate(['/dashboard/agent']); break;
      case 'REVIEWER': this.router.navigate(['/dashboard/reviewer']); break;
      case 'SELF_SERVICE_USER': this.router.navigate(['/dashboard/user']); break;
      default: this.router.navigate(['/login']); break;
    }
  }

  getUserRole(): string | null {
    const decoded = this.getDecodedToken();
    return decoded ? decoded.role : null;
  }

  getUserStatus(): string | null {
    const decoded = this.getDecodedToken();
    return decoded?.status || null;
  }

  getTenantId(): string | null {
    const decoded = this.getDecodedToken();
    return decoded?.tenantId || null;
  }

  private getDecodedToken(): DecodedToken | null {
    const token = this.getAccessToken();
    if (!token) {
      return null;
    }
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      console.error('Failed to decode token', e);
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const decoded = JSON.parse(atob(token.split('.')[1]));
      const expiry = decoded.exp * 1000;
      return Date.now() >= expiry;
    } catch (e) {
      return true;
    }
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error occurred';
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || `Error: ${error.status}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}