import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from "@angular/common/http";
import { inject } from "@angular/core";
import { BehaviorSubject, Observable, throwError } from "rxjs";
import { catchError, filter, switchMap, take } from "rxjs/operators";
import { AuthService } from "./auth.service";

// --- State for handling token refresh ---
let isRefreshing = false;
const refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

export const authInterceptor: HttpInterceptorFn = (
    req: HttpRequest<unknown>, 
    next: HttpHandlerFn
    ): Observable<HttpEvent<unknown>> => {

    const authService = inject(AuthService);
    const accessToken = authService.getAccessToken();

    // UPDATED: Do not intercept requests for login or token refresh
    if (req.url.includes('/auth/authenticate') || req.url.includes('/auth/refreshtoken')) {
        return next(req);
    }

    // Clone the request and add the authorization header if a token exists
    if (accessToken) {
        req = addTokenToRequest(req, accessToken);
    }

    return next(req).pipe(
        catchError(error => {
            // Intercept 401 Unauthorized responses
            if (error instanceof HttpErrorResponse && error.status === 401) {
                return handle401Error(req, next, authService);
            }
            return throwError(() => error);
        })
    );
};

// --- Helper function to add the token ---
const addTokenToRequest = (request: HttpRequest<any>, token: string) => {
    return request.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });
};

// --- Helper function to manage the 401 error and refresh process ---
const handle401Error = (
    request: HttpRequest<any>, 
    next: HttpHandlerFn,
    authService: AuthService
    ): Observable<HttpEvent<any>> => {
    
    // If not already refreshing, start the refresh process
    if (!isRefreshing) {
        isRefreshing = true;
        refreshTokenSubject.next(null);

        return authService.refreshToken().pipe(
            switchMap((tokenResponse: any) => {
                isRefreshing = false;
                refreshTokenSubject.next(tokenResponse.accessToken);
                // Retry the original request with the new token
                return next(addTokenToRequest(request, tokenResponse.accessToken));
            }),
            catchError((err) => {
                isRefreshing = false;
                authService.logout(); // Logout if refresh fails
                return throwError(() => err);
            })
        );
    } else {
        // If already refreshing, wait for the new token
        return refreshTokenSubject.pipe(
            filter(token => token != null),
            take(1),
            switchMap(jwt => {
                return next(addTokenToRequest(request, jwt));
            })
        );
    }
};