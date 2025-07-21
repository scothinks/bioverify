import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, timer, switchMap, takeWhile, tap } from 'rxjs';
import { TenantService } from '../services/tenant.service';
import { BulkJob } from '../models/bulk-job.model';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-bulk-verification',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './bulk-verification.component.html',
  styleUrls: ['./bulk-verification.component.scss']
})
export class BulkVerificationComponent implements OnInit, OnDestroy {
  jobs: BulkJob[] = [];
  isLoading = false;
  isPolling = false;
  displayedColumns: string[] = ['status', 'totalRecords', 'processed', 'createdAt', 'updatedAt'];
  private pollingSubscription?: Subscription;

  constructor(private tenantService: TenantService) { }

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.isLoading = true;
    this.tenantService.getBulkJobs().subscribe({
      next: (jobs) => {
        this.jobs = jobs;
        this.isLoading = false;
        // If there are active jobs, start polling
        if (jobs.some(job => job.status === 'RUNNING' || job.status === 'PENDING') && !this.isPolling) {
          this.startPolling();
        }
      },
      error: (err) => {
        console.error('Failed to load bulk jobs', err);
        this.isLoading = false;
      }
    });
  }

  startVerification(): void {
    this.isLoading = true;
    this.tenantService.startBulkVerification().subscribe({
      next: () => {
        alert('Bulk verification process has been initiated.');
        this.loadJobs(); // Refresh the list immediately
      },
      error: (err) => {
        alert('Failed to start bulk verification: ' + err.message);
        this.isLoading = false;
      }
    });
  }

  startPolling(): void {
    this.isPolling = true;
    this.pollingSubscription = timer(0, 10000) // Poll every 10 seconds
      .pipe(
        switchMap(() => this.tenantService.getBulkJobs()),
        tap(jobs => {
          this.jobs = jobs;
          // Stop polling if no jobs are active
          if (!jobs.some(job => job.status === 'RUNNING' || job.status === 'PENDING')) {
            this.stopPolling();
          }
        })
      ).subscribe();
  }

  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.isPolling = false;
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}