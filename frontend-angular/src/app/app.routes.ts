import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { EmployeeRegistrationComponent } from './employee-registration/employee-registration.component';
import { VerificationComponent } from './verification/verification.component';
import { AgentDashboardComponent } from './agent-dashboard/agent-dashboard.component'; // <-- IMPORT ADDED

export const routes: Routes = [
  // When the user is at the base path, redirect them to the login page.
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Define the routes for public-facing pages.
  { path: 'login', component: LoginComponent },
  { path: 'register-employee', component: EmployeeRegistrationComponent },

  // Define routes for authenticated users.
  { path: 'verification', component: VerificationComponent },
  { path: 'agent-dashboard', component: AgentDashboardComponent }, // <-- ROUTE ADDED

  // A wildcard route to redirect any unknown URLs back to login.
  { path: '**', redirectTo: 'login' }
];