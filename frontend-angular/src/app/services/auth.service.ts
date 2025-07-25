import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { MasterListRecord } from '../models/master-list-record.model';
import { Router } from '@angular/router';

export interface AuthResponse {
  token: string;
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
  private readonly TOKEN_KEY = 'bioverify_auth_token';

  private isLoggedInSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authApiUrl}/authenticate`, credentials).pipe(
      tap(response => {
        this.setToken(response.token);
        this.redirectUserBasedOnRole();
      }),
      catchError(this.handleError)
    );
  }

  /**
   * For an ADMIN to create a new administrative user (e.g., Tenant Manager, Focal Officer).
   * Does NOT log in as the created user.
   */
  register(userData: AuthRequest): Observable<any> {
    return this.http.post<any>(`${this.authApiUrl}/register`, userData).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * For an EMPLOYEE to create their own account after their identity has been successfully verified.
   */
  createAccount(accountData: AuthRequest): Observable<any> {
    return this.http.post<any>(`${this.authApiUrl}/create-account`, accountData).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * @deprecated This method uses the old combined registration/verification flow and should be removed.
   */
  registerEmployee(userData: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.authApiUrl}/register-employee`, userData).pipe(
      tap(response => this.setToken(response.token)),
      catchError(this.handleError)
    );
  }

  /**
   * Fetches the full MasterListRecord for the currently logged-in user.
   */
  getCurrentUserRecord(): Observable<MasterListRecord> {
    return this.http.get<MasterListRecord>(`${this.v1ApiUrl}/users/me/record`).pipe(
        catchError(this.handleError)
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.isLoggedInSubject.next(false);
    this.router.navigate(['/login']);
  }

  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.isLoggedInSubject.next(true);
  }

  /**
   * Navigates the user to the correct dashboard based on their role.
   */
  public redirectUserBasedOnRole(): void {
    const role = this.getUserRole();
    switch (role) {
      case 'GLOBAL_SUPER_ADMIN':
        this.router.navigate(['/dashboard/global-admin']);
        break;
      case 'TENANT_ADMIN':
        this.router.navigate(['/dashboard/tenant-admin']);
        break;
      // --- UPDATED SECTION ---
      case 'AGENT': // CORRECTED: Changed from 'Agent' to 'AGENT'
        this.router.navigate(['/dashboard/agent']); // CORRECTED: path is lowercase
        break;
      case 'REVIEWER': // NEW: Added case for the Reviewer role
        this.router.navigate(['/dashboard/reviewer']);
        break;
      // --- END UPDATED SECTION ---
      case 'SELF_SERVICE_USER':
        this.router.navigate(['/dashboard/user']);
        break;
      default:
        // Fallback to login if role is unknown or null
        this.router.navigate(['/login']);
        break;
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
        return false;
    }
    return !this.isTokenExpired(token);
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
    return decoded && decoded.tenantId ? decoded.tenantId : null;
  }

  private getDecodedToken(): DecodedToken | null {
    const token = this.getToken();
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
      switch (error.status) {
        case 401:
          errorMessage = 'Invalid credentials';
          break;
        case 403:
          errorMessage = 'Access denied';
          break;
        case 404:
          errorMessage = 'Service not found';
          break;
        case 500:
          errorMessage = 'Server error. Please try again later.';
          break;
        default:
          errorMessage = error.error?.message || `Error: ${error.status}`;
      }
    }
    return throwError(() => new Error(errorMessage));
  }
}