import { Component, OnDestroy, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, timer, switchMap } from 'rxjs';
import { TenantService, BulkVerificationJob } from '../services/tenant.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-bulk-verification',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe, // Added for formatting dates in the template
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule // Added for user feedback
  ],
  templateUrl: './bulk-verification.component.html',
  styleUrls: ['./bulk-verification.component.scss']
})
export class BulkVerificationComponent implements OnInit, OnDestroy, AfterViewInit {
  // --- UPDATED PROPERTIES FOR DASHBOARD VIEW ---
  jobHistoryDataSource = new MatTableDataSource<BulkVerificationJob>();
  isLoading = true;
  isPolling = false;
  isStartingJob = false;
  displayedColumns: string[] = ['initiatedAt', 'totalRecords', 'successfullyVerifiedRecords', 'failedRecords', 'status', 'actions'];
  totalVerified = 0;
  totalNotFound = 0;
  private pollingSubscription?: Subscription;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private tenantService: TenantService,
    private router: Router, // Injected Router for navigation
    private snackBar: MatSnackBar // Injected MatSnackBar for feedback
  ) {}

  ngOnInit(): void {
    this.loadJobHistory();
  }

  ngAfterViewInit() {
    this.jobHistoryDataSource.paginator = this.paginator;
  }

  // --- UPDATED TO FETCH FULL HISTORY AND CALCULATE TOTALS ---
  loadJobHistory(): void {
    this.isLoading = true;
    this.tenantService.getBulkJobHistory().subscribe({
      next: (jobs) => {
        this.jobHistoryDataSource.data = jobs;
        this.calculateTotals(jobs);
        this.isLoading = false;
        if (this.shouldPoll(jobs) && !this.isPolling) {
          this.startPolling();
        } else if (!this.shouldPoll(jobs) && this.isPolling) {
          this.stopPolling();
        }
      },
      error: (err) => {
        console.error('Failed to load bulk jobs', err);
        this.isLoading = false;
      }
    });
  }

  // --- NEW METHOD TO CALCULATE OVERALL STATS ---
  private calculateTotals(jobs: BulkVerificationJob[]): void {
    this.totalVerified = jobs.reduce((sum, job) => sum + job.successfullyVerifiedRecords, 0);
    this.totalNotFound = jobs.reduce((sum, job) => sum + job.failedRecords, 0);
  }

  // --- UPDATED FOR BETTER UX ---
  startNewJob(): void {
    this.isStartingJob = true;
    this.tenantService.startBulkVerification().subscribe({
      next: () => {
        this.snackBar.open('Bulk verification job has been initiated.', 'Close', { duration: 3000 });
        // Refresh the list after a short delay to show the new PENDING job
        setTimeout(() => this.loadJobHistory(), 1500);
        this.isStartingJob = false;
      },
      error: (err) => {
        this.snackBar.open('Failed to start bulk verification: ' + err.message, 'Close', { duration: 5000 });
        this.isStartingJob = false;
      }
    });
  }

  // --- POLLING LOGIC (UNCHANGED CORE, UPDATED SERVICE CALL) ---
  startPolling(): void {
    if (this.isPolling) return;
    this.isPolling = true;
    this.pollingSubscription = timer(5000, 10000)
      .pipe(
        switchMap(() => this.tenantService.getBulkJobHistory())
      ).subscribe(jobs => {
        this.jobHistoryDataSource.data = jobs;
        this.calculateTotals(jobs);
        if (!this.shouldPoll(jobs)) {
          this.stopPolling();
        }
      });
  }

  stopPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.isPolling = false;
  }

  private shouldPoll(jobs: BulkVerificationJob[]): boolean {
    return jobs.some(job => job.status === 'RUNNING' || job.status === 'PENDING');
  }

  // --- NEW NAVIGATION METHODS ---
  goToValidationQueue(): void {
    this.router.navigate(['/dashboard/tenant-admin/validation']);
  }

  goToNotFoundList(): void {
    this.router.navigate(['/dashboard/tenant-admin/not-found']);
  }
  
  // --- HELPER METHODS (UNCHANGED) ---
  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
  
  getStatusIcon(status: string): string {
    switch(status) {
      case 'COMPLETED': return 'check_circle';
      case 'FAILED': return 'error';
      case 'RUNNING': return 'autorenew';
      case 'PENDING': return 'hourglass_empty';
      default: return 'help';
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}