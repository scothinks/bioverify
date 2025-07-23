import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';
import { TenantService, DashboardStats } from '../services/tenant.service';

@Component({
  selector: 'app-tenant-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatGridListModule
  ],
  templateUrl: './tenant-overview.component.html',
  styleUrls: ['./tenant-overview.component.scss']
})
export class TenantOverviewComponent implements OnInit {
  
  public stats: DashboardStats | null = null;
  public isLoading = true;

  constructor(
    private tenantService: TenantService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.tenantService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load dashboard stats:', err);
        this.isLoading = false;
      }
    });
  }

  // Helper method for navigation
  navigate(path: string): void {
    this.router.navigate(['/dashboard/tenant-admin', path]);
  }
}