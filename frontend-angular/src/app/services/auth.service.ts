import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

// Keep your existing interfaces
export interface AuthResponse {
  token: string;
}

export interface AuthRequest {
  email?: string;
  password?: string;
  fullName?: string;
  role?: string;
}

// NEW: Interface for the decoded JWT payload
export interface DecodedToken {
  sub: string; // Subject (email)
  role: string;
  tenantId?: string; // tenantId is optional in the token
  iat: number; // Issued at
  exp: number; // Expiration time
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/v1/auth';
  private readonly TOKEN_KEY = 'bioverify_auth_token';

  constructor(private http: HttpClient) { }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/authenticate`, credentials).pipe(
      tap(response => this.setToken(response.token)),
      catchError(this.handleError)
    );
  }

  register(userData: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData).pipe(
      tap(response => this.setToken(response.token)),
      catchError(this.handleError)
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    // Reload to reset the app state
    window.location.reload();
  }

  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
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

  // --- NEW METHODS FOR JWT DECODING ---

  /**
   * Decodes the JWT and returns the user's role.
   * @returns The user's role as a string, or null if not logged in.
   */
  getUserRole(): string | null {
    const decoded = this.getDecodedToken();
    return decoded ? decoded.role : null;
  }

  /**
   * Decodes the JWT and returns the user's tenant ID.
   * @returns The user's tenant ID, or null if not available.
   */
  getTenantId(): string | null {
    const decoded = this.getDecodedToken();
    // FIX: Check if tenantId exists, otherwise return null.
    return decoded && decoded.tenantId ? decoded.tenantId : null;
  }

  /**
   * Helper method to decode the JWT from local storage.
   * @returns The decoded token payload, or null if token is invalid or not present.
   */
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

  /**
   * Checks if a token is expired by looking at its 'exp' claim.
   * @param token The JWT string.
   * @returns True if the token is expired, false otherwise.
   */
  private isTokenExpired(token: string): boolean {
    try {
      const decoded = JSON.parse(atob(token.split('.')[1]));
      const expiry = decoded.exp * 1000;
      return Date.now() >= expiry;
    } catch (e) {
      return true;
    }
  }

  // --- END OF NEW METHODS ---

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
