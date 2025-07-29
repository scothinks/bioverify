import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MasterListRecordDto } from '../services/pol.service';
import { TenantService } from '../services/tenant.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-document-review-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatCardModule, MatButtonModule,
    MatIconModule, MatDividerModule, MatProgressSpinnerModule, MatSnackBarModule
  ],
  templateUrl: './document-review-dialog.component.html',
  styleUrls: ['./document-review-dialog.component.scss']
})
export class DocumentReviewDialogComponent {
  
  public isLoading = false;

  constructor(
    private dialogRef: MatDialogRef<DocumentReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public record: MasterListRecordDto,
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  // Admin manually approves the record, moving it to 'REVIEWED' status
  approve(): void {
    this.isLoading = true;
    // We re-use the 'validateRecord' endpoint for this admin action
    this.tenantService.validateRecord(this.record.id, 'REVIEWED', 'Manually approved by admin after invalid document flag.').subscribe({
      next: () => {
        this.isLoading = false;
        this.dialogRef.close('updated');
      },
      error: (err) => {
        this.isLoading = false;
        this.showSnackBar('Failed to approve the record.', 'error');
      }
    });
  }

  // Admin rejects the record, moving it to 'REJECTED' status
  reject(): void {
    const reason = prompt('Please provide a reason for this rejection:');
    if (reason && reason.trim()) {
      this.isLoading = true;
      this.tenantService.validateRecord(this.record.id, 'REJECTED', reason).subscribe({
        next: () => {
          this.isLoading = false;
          this.dialogRef.close('updated');
        },
        error: (err) => {
          this.isLoading = false;
          this.showSnackBar('Failed to reject the record.', 'error');
        }
      });
    }
  }

  close(): void {
    this.dialogRef.close();
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