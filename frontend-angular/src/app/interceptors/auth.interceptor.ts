import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpInterceptor,
  HttpHandler,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Intercepts all outgoing HTTP requests and adds the JWT Authorization header
 * if the user is logged in.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get the auth token from the service.
    const authToken = this.authService.getToken();

    // If a token exists, clone the request and add the new header.
    if (authToken) {
      const authReq = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${authToken}`)
      });
      // Pass the cloned request instead of the original request.
      return next.handle(authReq);
    }

    // If there is no token, pass the original request along.
    return next.handle(req);
  }
}
