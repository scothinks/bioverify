import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
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
    MatTabsModule,
    MatPaginatorModule
  ],
  templateUrl: './validation-queue.component.html',
  styleUrl: './validation-queue.component.scss'
})
export class ValidationQueueComponent implements OnInit {
  
  public pendingRecords = new MatTableDataSource<MasterListRecord>();
  public mismatchedRecords = new MatTableDataSource<MasterListRecord>();
  
  public displayedColumns: string[] = [
    'fullName', 
    'department', 
    'ministry', 
    'gradeLevel',
    'salaryGradeId',
    'salaryStructure', 
    'actions'
  ];
  public selectedTabIndex = 0;

  // 1. REMOVE the old @ViewChild properties
  // @ViewChild('pendingPaginator') pendingPaginator!: MatPaginator;
  // @ViewChild('mismatchedPaginator') mismatchedPaginator!: MatPaginator;

  // 2. REPLACE with these setters. This is the key change.
  @ViewChild('pendingPaginator') set pendingPaginator(paginator: MatPaginator) {
    if (paginator) {
      this.pendingRecords.paginator = paginator;
    }
  }

  @ViewChild('mismatchedPaginator') set mismatchedPaginator(paginator: MatPaginator) {
    if (paginator) {
      this.mismatchedRecords.paginator = paginator;
    }
  }

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
        next: (data) => {
          // 3. Assign data. The setter will handle the paginator automatically.
          this.pendingRecords.data = data;
        },
        error: (error) => this.showSnackBar('Failed to load pending queue', 'error')
      });
    } else {
      this.tenantService.getMismatchedQueue().subscribe({
        next: (data) => {
          // 4. Assign data. The setter will handle the paginator automatically.
          this.mismatchedRecords.data = data;
        },
        error: (error) => this.showSnackBar('Failed to load mismatched queue', 'error')
      });
    }
  }
  
  // ... All other methods remain the same ...

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