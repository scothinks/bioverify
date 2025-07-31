// src/app/services/auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { MasterListRecordDto } from './pol.service';

// NEW: Matches the new JwtResponse DTO from the backend
export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  type: string;
  id: string;
  email: string;
  role: string;
}

export interface AuthRequest {
  email?: string;
  password?: string;
  fullName?: string;
  role?: string;
  ssid?: string;
  nin?: string;
  recordId?: string;
}

export interface DecodedToken {
  sub: string;
  role: string;
  tenantId?: string;
  status?: string;
  iat: number;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authApiUrl = 'http://localhost:8080/api/v1/auth';
  private v1ApiUrl = 'http://localhost:8080/api/v1';

  // UPDATED: Separate keys for each token
  private readonly ACCESS_TOKEN_KEY = 'bioverify_access_token';
  private readonly REFRESH_TOKEN_KEY = 'bioverify_refresh_token';

  private isLoggedInSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  // UPDATED: Handles the new JwtResponse and stores both tokens
  login(credentials: AuthRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.authApiUrl}/authenticate`, credentials).pipe(
      tap(response => {
        this.setAccessToken(response.accessToken);
        this.setRefreshToken(response.refreshToken);
        this.isLoggedInSubject.next(true);
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