import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TenantService, PayrollExportLog } from '../services/tenant.service';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-payroll-export',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './payroll-export.component.html',
  styleUrls: ['./payroll-export.component.scss']
})
export class PayrollExportComponent implements OnInit, OnDestroy, AfterViewInit {
  public dataSource = new MatTableDataSource<PayrollExportLog>();
  public displayedColumns: string[] = ['status', 'recordCount', 'exportedByEmail', 'exportTimestamp', 'actions'];
  public isLoading = true;
  public isGenerating = false;
  private pollingSubscription?: Subscription;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.tenantService.getExportHistory().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.isLoading = false;
        if (this.shouldPoll(data)) {
          this.startPolling();
        } else {
          this.stopPolling();
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.showSnackBar('Failed to load export history.', 'error-snackbar');
        console.error(err);
      }
    });
  }

  generateNewExport(): void {
    this.isGenerating = true;
    this.tenantService.initiateExport().subscribe({
      next: () => {
        this.isGenerating = false;
        this.showSnackBar('Payroll export job initiated! The list will update shortly.', 'success-snackbar');
        setTimeout(() => this.loadHistory(), 3000);
      },
      error: (err) => {
        this.isGenerating = false;
        this.showSnackBar('Failed to initiate export.', 'error-snackbar');
        console.error(err);
      }
    });
  }

  downloadFile(log: PayrollExportLog): void {
    this.tenantService.downloadExportFile(log.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payroll-export-${log.id}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (err) => {
        this.showSnackBar('Failed to download file.', 'error-snackbar');
        console.error(err);
      }
    });
  }
  
  startPolling(): void {
    if (this.pollingSubscription && !this.pollingSubscription.closed) return;
    
    this.pollingSubscription = timer(5000, 15000)
      .pipe(switchMap(() => this.tenantService.getExportHistory()))
      .subscribe(data => {
        this.dataSource.data = data;
        if (!this.shouldPoll(data)) {
          this.stopPolling();
        }
      });
  }

  stopPolling(): void {
    this.pollingSubscription?.unsubscribe();
  }

  shouldPoll(jobs: PayrollExportLog[]): boolean {
    return jobs.some(job => job.status === 'PENDING' || job.status === 'RUNNING');
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'check_circle';
      case 'FAILED': return 'error';
      case 'RUNNING': return 'autorenew';
      case 'PENDING': return 'hourglass_empty';
      default: return 'help';
    }
  }

  private showSnackBar(message: string, panelClass: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: [panelClass]
    });
  }
}