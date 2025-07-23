import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TenantService } from '../services/tenant.service';
import { MasterListRecord } from '../models/master-list-record.model';
import { RecordEditFormComponent } from '../record-edit-form/record-edit-form.component';
import { RecordMismatchDialogComponent } from '../record-mismatch-dialog/record-mismatch-dialog.component';
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-validation-queue',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTabsModule
  ],
  templateUrl: './validation-queue.component.html',
  styleUrl: './validation-queue.component.scss'
})
export class ValidationQueueComponent implements OnInit {
  
  public pendingRecords: MasterListRecord[] = [];
  public mismatchedRecords: MasterListRecord[] = [];
  
  // 'status' has been removed from this array to hide the column
  public displayedColumns: string[] = [
    'fullName', 
    'department', 
    'ministry', 
    'gradeLevel', 
    'salaryStructure', 
    'actions'
  ];
  public selectedTabIndex = 0;

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDataForCurrentTab();
  }

  loadDataForCurrentTab(): void {
    if (this.selectedTabIndex === 0) {
      this.tenantService.getPendingApprovalQueue().subscribe({
        next: (data) => this.pendingRecords = data,
        error: (error) => this.showSnackBar('Failed to load pending queue', 'error')
      });
    } else {
      this.tenantService.getMismatchedQueue().subscribe({
        next: (data) => this.mismatchedRecords = data,
        error: (error) => this.showSnackBar('Failed to load mismatched queue', 'error')
      });
    }
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    this.loadDataForCurrentTab();
  }

  onApprove(recordId: string): void {
    this.tenantService.validateRecord(recordId, 'VALIDATED', 'Approved by reviewer.').subscribe({
      next: () => {
        this.showSnackBar('Record approved successfully!', 'success');
        this.loadDataForCurrentTab();
      },
      error: (error) => this.showSnackBar('Failed to approve record', 'error')
    });
  }

  onReject(recordId: string): void {
    const reason = prompt('Please provide a reason for rejection:');
    if (reason?.trim()) {
      this.tenantService.validateRecord(recordId, 'REJECTED', reason).subscribe({
        next: () => {
          this.showSnackBar('Record rejected successfully!', 'success');
          this.loadDataForCurrentTab();
        },
        error: (error) => this.showSnackBar('Failed to reject record', 'error')
      });
    }
  }

  onEdit(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordEditFormComponent, {
      width: '600px',
      data: record
    });

    dialogRef.afterClosed().subscribe(wasUpdated => {
      if (wasUpdated) {
        this.showSnackBar('Record updated successfully!', 'success');
        this.loadDataForCurrentTab();
      }
    });
  }

  onResolve(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordMismatchDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      data: record
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === 'accept') {
        this.tenantService.resolveMismatch(record.id).subscribe({
          next: () => {
            this.showSnackBar('Record resolved and validated successfully!', 'success');
            this.loadDataForCurrentTab();
          },
          error: (err) => this.showSnackBar('Failed to resolve record.', 'error')
        });
      } else if (result === 'edit') {
        this.onEdit(record);
      }
    });
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) {
      return names[0].charAt(0).toUpperCase();
    }
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }
  
  getStatusClass(status: string): string {
    const statusLower = status?.toLowerCase() || '';
    switch (statusLower) {
      case 'pending': return 'pending';
      case 'approved': case 'validated': return 'approved';
      case 'rejected': return 'rejected';
      default: return 'pending';
    }
  }

  getStatusIcon(status: string): string {
    const statusLower = status?.toLowerCase() || '';
    switch (statusLower) {
      case 'pending': return 'schedule';
      case 'approved': case 'validated': return 'check_circle';
      case 'rejected': return 'cancel';
      default: return 'help';
    }
  }

  private showSnackBar(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const config = { 
      duration: 4000, 
      horizontalPosition: 'end' as const, 
      verticalPosition: 'top' as const, 
      panelClass: [`snackbar-${type}`] 
    };
    this.snackBar.open(message, 'Close', config);
  }
}