import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { EmployeeRegistrationComponent } from './employee-registration/employee-registration.component';
// import { VerificationComponent } from './verification/verification.component'; // REMOVED: No longer needed
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

// Import the layout dashboard components
import { GlobalAdminDashboardComponent } from './dashboards/global-admin-dashboard/global-admin-dashboard.component';
import { TenantAdminDashboardComponent } from './dashboards/tenant-admin-dashboard/tenant-admin-dashboard.component';
import { AgentDashboardComponent } from './dashboards/agent-dashboard/agent-dashboard.component';
import { SelfServiceUserDashboardComponent } from './dashboards/self-service-user-dashboard/self-service-user-dashboard.component';


// Import the child components that will now be routed as screens
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { UserListComponent } from './user-list/user-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { MasterListComponent } from './master-list/master-list.component';
import { BulkVerificationComponent } from './bulk-verification/bulk-verification.component';
import { UserDashboardComponent } from './user-dashboard/user-dashboard.component';
import { ValidationQueueComponent } from './validation-queue/validation-queue.component';
import { PayrollExportComponent } from './payroll-export/payroll-export.component';
import { NotFoundRecordsComponent } from './not-found-records/not-found-records.component';
import { TenantOverviewComponent } from './tenant-overview/tenant-overview.component';
import { ReviewerDashboardComponent } from './dashboards/reviewer-dashboard/reviewer-dashboard.component';

export const routes: Routes = [
  // Public routes
  { path: 'login', component: LoginComponent },
  { path: 'register-employee', component: EmployeeRegistrationComponent },
  // { path: 'verification', component: VerificationComponent }, // REMOVED: This route is obsolete

  // Main dashboard parent route, protected by the auth guard
  {
    path: 'dashboard',
    canActivate: [authGuard],
    children: [
      {
        path: 'global-admin',
        component: GlobalAdminDashboardComponent,
        canActivate: [roleGuard],
        data: { expectedRole: 'GLOBAL_SUPER_ADMIN' },
        children: [
          { path: 'tenants', component: TenantListComponent },
          { path: '', redirectTo: 'tenants', pathMatch: 'full' }
        ]
      },
      {
        path: 'tenant-admin',
        component: TenantAdminDashboardComponent,
        canActivate: [roleGuard],
        data: { expectedRole: 'TENANT_ADMIN' },
        children: [
          { path: 'overview', component: TenantOverviewComponent },
          { path: 'users', component: UserListComponent },
          { path: 'uploads', component: FileUploadComponent },
          { path: 'records', component: MasterListComponent },
          { path: 'bulk-verify', component: BulkVerificationComponent },
          { path: 'validation', component: ValidationQueueComponent },
          { path: 'export', component: PayrollExportComponent },
          { path: 'not-found', component: NotFoundRecordsComponent },
          { path: '', redirectTo: 'overview', pathMatch: 'full' }
        ]
      },
      {
        path: 'agent',
        component: AgentDashboardComponent,
        canActivate: [roleGuard],
        data: { expectedRole: 'AGENT' },
        children: [
          // This can be expanded with agent-specific tasks
          { path: 'verify', component: EmployeeRegistrationComponent }, // Agents can use the same component
          { path: '', redirectTo: 'verify', pathMatch: 'full' }
        ]
      },
      // NEW: Add dashboard for the Reviewer role
      {
        path: 'reviewer',
        component: ReviewerDashboardComponent, // This is a new shell component
        canActivate: [roleGuard],
        data: { expectedRole: 'REVIEWER' },
        children: [
          { path: 'queue', component: ValidationQueueComponent },
          { path: '', redirectTo: 'queue', pathMatch: 'full' }
        ]
      },
      {
        path: 'user',
        component: SelfServiceUserDashboardComponent,
        canActivate: [roleGuard],
        data: { expectedRole: 'SELF_SERVICE_USER' },
        children: [
          { path: 'profile', component: UserDashboardComponent },
          { path: '', redirectTo: 'profile', pathMatch: 'full' }
        ]
      }
    ]
  },

  // Default and wildcard routes
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];