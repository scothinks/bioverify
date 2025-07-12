// FILE: src/app/app.config.ts

import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
// Import the necessary providers
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor'; // <-- IMPORT

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // Register the HttpClient and the Interceptor
    provideHttpClient(withInterceptorsFromDi()), // <-- MODIFY THIS LINE
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true } // <-- ADD THIS
  ]
};