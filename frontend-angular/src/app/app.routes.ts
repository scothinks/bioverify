import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { EmployeeRegistrationComponent } from './employee-registration/employee-registration.component';
import { VerificationComponent } from './verification/verification.component'; // <-- IMPORT ADDED

export const routes: Routes = [
  // When the user is at the base path, redirect them to the login page.
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Define the routes for the login and registration pages.
  { path: 'login', component: LoginComponent },
  { path: 'register-employee', component: EmployeeRegistrationComponent },

  // Add the route for the verification page for existing users.
  { path: 'verification', component: VerificationComponent },

  // Optional: A wildcard route to redirect any unknown URLs back to login.
  { path: '**', redirectTo: 'login' }
];