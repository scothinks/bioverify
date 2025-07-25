import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-global-performance-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatGridListModule,
    MatIconModule
  ],
  templateUrl: './global-performance-overview.component.html',
  styleUrls: ['./global-performance-overview.component.scss']
})
export class GlobalPerformanceOverviewComponent {
  // --- Placeholder Data ---

  // System Health Metrics
  systemStatus = 'Operational';
  apiUptime = '99.98%';
  avgResponseTime = '120ms';
  errorRate = '0.02%';

  // Platform Metrics
  activeTenants = 12;
  totalVerifications = '1.45M';
  totalUsers = '89.5K';
  subscriptionStatus = '10 Active / 2 Trial';

  constructor() { }
}