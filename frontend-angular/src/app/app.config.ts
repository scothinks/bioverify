import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './services/auth.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer'; // NEW IMPORT

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])), 
    provideAnimationsAsync(),
    NgxExtendedPdfViewerModule // NEW PROVIDER
  ]
};