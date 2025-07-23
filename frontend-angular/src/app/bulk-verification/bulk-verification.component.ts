import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
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
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './bulk-verification.component.html',
  styleUrls: ['./bulk-verification.component.scss']
})
export class BulkVerificationComponent implements OnInit, OnDestroy {
  jobHistoryDataSource = new MatTableDataSource<BulkVerificationJob>();
  isLoading = true;
  isPolling = false;
  isStartingJob = false;
  displayedColumns: string[] = ['createdAt', 'totalRecords', 'successfullyVerifiedRecords', 'failedRecords', 'status', 'actions'];
  totalVerified = 0;
  totalNotFound = 0;
  private pollingSubscription?: Subscription;

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    if (paginator) {
      this.jobHistoryDataSource.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadJobHistory();
  }

  loadJobHistory(): void {
    this.isLoading = true;
    this.tenantService.getBulkJobHistory().subscribe({
      next: (jobs) => {
        const processedJobs = this.processJobs(jobs);
        this.jobHistoryDataSource.data = processedJobs;
        this.calculateTotals(processedJobs);
        this.isLoading = false;
        if (this.shouldPoll(processedJobs) && !this.isPolling) {
          this.startPolling();
        } else if (!this.shouldPoll(processedJobs) && this.isPolling) {
          this.stopPolling();
        }
      },
      error: (err) => {
        console.error('Failed to load bulk jobs', err);
        this.isLoading = false;
        this.snackBar.open('Could not load job history.', 'Close', { duration: 3000 });
      }
    });
  }
  
  // FIX 1: Use a type assertion ('any') to resolve the Date/string type mismatch.
  private processJobs(jobs: BulkVerificationJob[]): BulkVerificationJob[] {
    return jobs.map(job => {
      const newJob: any = { ...job };
      newJob.createdAt = new Date(job.createdAt);
      return newJob;
    });
  }

  private calculateTotals(jobs: BulkVerificationJob[]): void {
    this.totalVerified = jobs.reduce((sum, job) => sum + job.successfullyVerifiedRecords, 0);
    this.totalNotFound = jobs.reduce((sum, job) => sum + job.failedRecords, 0);
  }

  startNewJob(): void {
    this.isStartingJob = true;
    this.tenantService.startBulkVerification().subscribe({
      next: () => {
        this.snackBar.open('Bulk verification job has been initiated.', 'Close', { duration: 3000 });
        setTimeout(() => this.loadJobHistory(), 1500);
        this.isStartingJob = false;
      },
      error: (err) => {
        this.snackBar.open('Failed to start bulk verification: ' + err.message, 'Close', { duration: 5000 });
        this.isStartingJob = false;
      }
    });
  }

  startPolling(): void {
    if (this.isPolling) return;
    this.isPolling = true;
    this.pollingSubscription = timer(5000, 10000)
      .pipe(
        switchMap(() => this.tenantService.getBulkJobHistory())
      ).subscribe(jobs => {
        const processedJobs = this.processJobs(jobs);
        this.jobHistoryDataSource.data = processedJobs;
        this.calculateTotals(processedJobs);
        if (!this.shouldPoll(processedJobs)) {
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

  goToValidationQueue(): void {
    this.router.navigate(['/dashboard/tenant-admin/validation']);
  }

  goToNotFoundList(): void {
    this.router.navigate(['/dashboard/tenant-admin/not-found']);
  }
  
  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }
  
  getStatusIcon(status: string): string {
    switch(status) {
      case 'COMPLETED': return 'check_circle';
      case 'FAILED': return 'error';
      case 'RUNNING': return 'autorenew';
      // FIX 2: Add the missing 'return' statement.
      case 'PENDING': return 'hourglass_empty';
      default: return 'help';
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}