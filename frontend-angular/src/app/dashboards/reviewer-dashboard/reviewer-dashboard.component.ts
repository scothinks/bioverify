// src/app/dashboards/reviewer-dashboard/reviewer-dashboard.component.ts

import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

// Import Dialog Components
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MasterListRecord } from '../../models/master-list-record.model';
import { TenantService } from '../../services/tenant.service';
import { RecordEditFormComponent } from '../../record-edit-form/record-edit-form.component';
import { RecordMismatchDialogComponent } from '../../record-mismatch-dialog/record-mismatch-dialog.component';

@Component({
  selector: 'app-reviewer-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './reviewer-dashboard.component.html',
  styleUrls: ['./reviewer-dashboard.component.scss']
})
export class ReviewerDashboardComponent implements OnInit {
  isLoading = true;
  pendingRecords = new MatTableDataSource<MasterListRecord>();
  mismatchedRecords = new MatTableDataSource<MasterListRecord>();

  displayedColumns: string[] = ['fullName', 'department', 'ministry', 'gradeLevel', 'actions'];
  selectedTabIndex = 0;

  @ViewChild('pendingPaginator') set pendingPaginator(paginator: MatPaginator) {
    if (paginator) { this.pendingRecords.paginator = paginator; }
  }

  @ViewChild('mismatchedPaginator') set mismatchedPaginator(paginator: MatPaginator) {
    if (paginator) { this.mismatchedRecords.paginator = paginator; }
  }

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadReviewerQueues();
  }

  loadReviewerQueues(): void {
    this.isLoading = true;
    forkJoin({
      // CORRECTED: Used the correct method names from the service
      pending: this.tenantService.getPendingApprovalQueue(),
      mismatched: this.tenantService.getMismatchedQueue()
    }).subscribe({
      next: ({ pending, mismatched }) => {
        this.pendingRecords.data = pending;
        this.mismatchedRecords.data = mismatched;
        this.isLoading = false;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.showSnackBar('Failed to load validation queues.', 'error');
        console.error(err);
      }
    });
  }

  onTabChange(): void {
    // Data for both tabs is loaded initially, so we may not need to reload unless desired.
  }

  onApprove(recordId: string): void {
    this.tenantService.validateRecord(recordId, 'VALIDATED', 'Approved by reviewer.').subscribe({
      next: () => {
        this.showSnackBar('Record approved successfully!', 'success');
        this.loadReviewerQueues();
      },
      error: (error: any) => this.showSnackBar('Failed to approve record', 'error')
    });
  }

  onReject(recordId: string): void {
    const reason = prompt('Please provide a reason for rejection:');
    if (reason?.trim()) {
      this.tenantService.validateRecord(recordId, 'REJECTED', reason).subscribe({
        next: () => {
          this.showSnackBar('Record rejected successfully!', 'success');
          this.loadReviewerQueues();
        },
        error: (error: any) => this.showSnackBar('Failed to reject record', 'error')
      });
    }
  }

  onEdit(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordEditFormComponent, {
      width: '600px',
      data: record
    });
    dialogRef.afterClosed().subscribe(wasUpdated => {
      if (wasUpdated) { this.loadReviewerQueues(); }
    });
  }

  onResolve(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordMismatchDialogComponent, {
      width: '800px',
      data: record
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === 'accept') {
        this.tenantService.resolveMismatch(record.id).subscribe({
          next: () => {
            this.showSnackBar('Record resolved and validated!', 'success');
            this.loadReviewerQueues();
          },
          error: (err: any) => this.showSnackBar('Failed to resolve record.', 'error')
        });
      } else if (result === 'edit') {
        this.onEdit(record);
      }
    });
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  private showSnackBar(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: [`snackbar-${type}`]
    });
  }
}