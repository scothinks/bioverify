import { Component, OnDestroy, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, timer, switchMap } from 'rxjs';
import { TenantService } from '../services/tenant.service';
import { BulkJob } from '../models/bulk-job.model';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
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
    MatPaginatorModule, // Added
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './bulk-verification.component.html',
  styleUrls: ['./bulk-verification.component.scss']
})
export class BulkVerificationComponent implements OnInit, OnDestroy, AfterViewInit {
  dataSource = new MatTableDataSource<BulkJob>();
  isLoading = true;
  isPolling = false;
  displayedColumns: string[] = ['status', 'progress', 'totalRecords', 'createdAt', 'actions'];
  private pollingSubscription?: Subscription;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(private tenantService: TenantService) { }

  ngOnInit(): void {
    this.loadJobs();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
  }

  loadJobs(): void {
    this.isLoading = true;
    this.tenantService.getBulkJobs().subscribe({
      next: (jobs) => {
        this.dataSource.data = jobs;
        this.isLoading = false;
        if (this.shouldPoll(jobs) && !this.isPolling) {
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
    this.tenantService.startBulkVerification().subscribe({
      next: () => {
        alert('Bulk verification process has been initiated.');
        this.loadJobs();
      },
      error: (err) => {
        alert('Failed to start bulk verification: ' + err.message);
      }
    });
  }

  startPolling(): void {
    this.isPolling = true;
    this.pollingSubscription = timer(5000, 10000) // Start after 5s, then every 10s
      .pipe(
        switchMap(() => this.tenantService.getBulkJobs())
      ).subscribe(jobs => {
        this.dataSource.data = jobs;
        if (!this.shouldPoll(jobs)) {
          this.stopPolling();
        }
      });
  }

  stopPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.isPolling = false;
  }

  private shouldPoll(jobs: BulkJob[]): boolean {
    return jobs.some(job => job.status === 'RUNNING' || job.status === 'PENDING');
  }
  
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