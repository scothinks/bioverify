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
    MatSnackBarModule
  ],
  templateUrl: './validation-queue.component.html',
  styleUrl: './validation-queue.component.scss'
})
export class ValidationQueueComponent implements OnInit {
  
  public records: MasterListRecord[] = [];
  public displayedColumns: string[] = [
    'fullName', 
    'department', 
    'ministry', 
    'gradeLevel', 
    'salaryStructure', 
    'status', 
    'actions'
  ];

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.tenantService.getValidationQueue().subscribe({
      next: (data) => {
        this.records = data;
      },
      error: (error) => {
        this.showSnackBar('Failed to load validation queue', 'error');
        console.error('Error loading validation queue:', error);
      }
    });
  }

  onApprove(recordId: string): void {
    this.tenantService.validateRecord(recordId, 'VALIDATED', 'Approved by reviewer.').subscribe({
      next: () => {
        this.records = this.records.filter(r => r.id !== recordId);
        this.showSnackBar('Record approved successfully!', 'success');
      },
      error: (error) => {
        this.showSnackBar('Failed to approve record', 'error');
        console.error('Error approving record:', error);
      }
    });
  }

  onReject(recordId: string): void {
    const reason = prompt('Please provide a reason for rejection:');
    if (reason?.trim()) {
      this.tenantService.validateRecord(recordId, 'REJECTED', reason).subscribe({
        next: () => {
          this.records = this.records.filter(r => r.id !== recordId);
          this.showSnackBar('Record rejected successfully!', 'success');
        },
        error: (error) => {
          this.showSnackBar('Failed to reject record', 'error');
          console.error('Error rejecting record:', error);
        }
      });
    }
  }

  onEdit(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordEditFormComponent, {
      width: '600px',
      maxWidth: '90vw',
      disableClose: false,
      autoFocus: true,
      data: record,
      panelClass: 'enhanced-dialog'
    });

    dialogRef.afterClosed().subscribe(updatedRecord => {
      if (updatedRecord) {
        const index = this.records.findIndex(r => r.id === updatedRecord.id);
        if (index !== -1) {
          const newRecords = [...this.records];
          newRecords[index] = updatedRecord;
          this.records = newRecords;
          this.showSnackBar('Record updated successfully!', 'success');
        }
      }
    });
  }

  // Helper method to get employee initials for avatar
  getInitials(fullName: string): string {
    if (!fullName) return '?';
    
    const names = fullName.trim().split(' ');
    if (names.length === 1) {
      return names[0].charAt(0).toUpperCase();
    }
    
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  // Helper method to get status-specific CSS class
  getStatusClass(status: string): string {
    const statusLower = status?.toLowerCase() || '';
    
    switch (statusLower) {
      case 'pending':
        return 'pending';
      case 'approved':
      case 'validated':
        return 'approved';
      case 'rejected':
        return 'rejected';
      default:
        return 'pending';
    }
  }

  // Helper method to get status-specific icon
  getStatusIcon(status: string): string {
    const statusLower = status?.toLowerCase() || '';
    
    switch (statusLower) {
      case 'pending':
        return 'schedule';
      case 'approved':
      case 'validated':
        return 'check_circle';
      case 'rejected':
        return 'cancel';
      default:
        return 'help';
    }
  }

  // Enhanced snackbar method with styling
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