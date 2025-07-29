import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { MasterListRecordDto } from '../services/pol.service';

// Import Angular Material Modules
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentReviewDialogComponent } from '../document-review-dialog/document-review-dialog.component';

@Component({
  selector: 'app-invalid-document-queue',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './invalid-document-queue.component.html',
  styleUrls: ['./invalid-document-queue.component.scss']
})
export class InvalidDocumentQueueComponent implements OnInit {
  
  public flaggedRecords = new MatTableDataSource<MasterListRecordDto>();
  public displayedColumns: string[] = ['fullName', 'ministryName', 'departmentName', 'actions'];
  public isLoading = true;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.isLoading = true;
    this.tenantService.getInvalidDocumentQueue().subscribe({
      next: (data) => {
        this.flaggedRecords.data = data;
        this.flaggedRecords.paginator = this.paginator;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.showSnackBar('Failed to load invalid document queue.', 'error');
        console.error(err);
      }
    });
  }

  onReview(record: MasterListRecordDto): void {
    const dialogRef = this.dialog.open(DocumentReviewDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      data: record
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === 'updated') {
        this.showSnackBar('Record status updated successfully.', 'success');
        this.loadQueue(); // Refresh the list after an action was taken
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

  private showSnackBar(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: type === 'success' ? 'success-snackbar' : 'error-snackbar'
    });
  }
}