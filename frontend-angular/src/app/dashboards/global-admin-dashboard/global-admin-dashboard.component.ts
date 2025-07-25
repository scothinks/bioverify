import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

// Import Angular Material Modules
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-global-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatTabsModule,
    MatIconModule
  ],
  templateUrl: './global-admin-dashboard.component.html',
  styleUrls: ['./global-admin-dashboard.component.scss']
})
export class GlobalAdminDashboardComponent {
  // Navigation links for the tab group, similar to the Tenant dashboard
  navLinks = [
    { path: 'overview', label: 'Overview', icon: 'dashboard' },
    { path: 'tenants', label: 'Tenant Management', icon: 'corporate_fare' }
  ];
}