import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { EmployeeRegistrationComponent } from './employee-registration/employee-registration.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

// Import the layout dashboard components
import { GlobalAdminDashboardComponent } from './dashboards/global-admin-dashboard/global-admin-dashboard.component';
import { TenantAdminDashboardComponent } from './dashboards/tenant-admin-dashboard/tenant-admin-dashboard.component';
import { AgentDashboardComponent } from './dashboards/agent-dashboard/agent-dashboard.component'; // This is the shell
import { SelfServiceUserDashboardComponent } from './dashboards/self-service-user-dashboard/self-service-user-dashboard.component';
import { ReviewerDashboardComponent } from './dashboards/reviewer-dashboard/reviewer-dashboard.component';

// Import the child components that will now be routed as screens
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { UserListComponent } from './user-list/user-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { MasterListComponent } from './master-list/master-list.component';
import { BulkVerificationComponent } from './bulk-verification/bulk-verification.component';
import { UserDashboardComponent } from './user-dashboard/user-dashboard.component';
import { ReviewQueueComponent } from './review-queue/review-queue.component';
import { PayrollExportComponent } from './payroll-export/payroll-export.component';
import { NotFoundRecordsComponent } from './not-found-records/not-found-records.component';
import { TenantOverviewComponent } from './tenant-overview/tenant-overview.component';
import { GlobalPerformanceOverviewComponent } from './dashboards/global-admin-dashboard/global-performance-overview/global-performance-overview.component';
import { InvalidDocumentQueueComponent } from './invalid-document-queue/invalid-document-queue.component';
import { AgentLayoutComponent } from './agent-layout/agent-layout.component'; // This is the content

export const routes: Routes = [
  // Public routes
  { path: 'login', component: LoginComponent },
  { path: 'register-employee', component: EmployeeRegistrationComponent },

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
          { path: 'overview', component: GlobalPerformanceOverviewComponent },
          { path: 'tenants', component: TenantListComponent },
          { path: '', redirectTo: 'overview', pathMatch: 'full' }
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
          { path: 'review-queue', component: ReviewQueueComponent },
          { path: 'invalid-documents', component: InvalidDocumentQueueComponent },
          { path: 'export', component: PayrollExportComponent },
          { path: 'not-found', component: NotFoundRecordsComponent },
          { path: '', redirectTo: 'overview', pathMatch: 'full' }
        ]
      },
      {
        path: 'agent',
        component: AgentDashboardComponent, // The shell
        canActivate: [roleGuard],
        data: { expectedRole: 'AGENT' },
        children: [
          // The content to load inside the shell's <router-outlet>
          { path: '', component: AgentLayoutComponent, pathMatch: 'full' }
        ]
      },
      {
        path: 'reviewer',
        component: ReviewerDashboardComponent,
        canActivate: [roleGuard],
        data: { expectedRole: 'REVIEWER' },
        children: [
          { path: 'queue', component: ReviewQueueComponent },
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