import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MasterListRecordDto } from '../services/pol.service';
import { TenantService } from '../services/tenant.service';
import { AuthService } from '../services/auth.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

// Import the PDF Viewer Module
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';

@Component({
  selector: 'app-document-review-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    NgxExtendedPdfViewerModule
  ],
  templateUrl: './document-review-dialog.component.html',
  styleUrls: ['./document-review-dialog.component.scss']
})
export class DocumentReviewDialogComponent {
  
  public isLoading = false;
  public fileBaseUrl = 'http://localhost:8080/files/';
  public httpHeaders: any;

  constructor(
    private dialogRef: MatDialogRef<DocumentReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public record: MasterListRecordDto,
    private tenantService: TenantService,
    private snackBar: MatSnackBar,
    private authService: AuthService
  ) {
    const token = this.authService.getAccessToken();
    if (token) {
      this.httpHeaders = {
        Authorization: `Bearer ${token}`
      };
    }
  }

  /**
   * UPDATED: This now calls the dedicated approval endpoint which activates the record
   * and triggers the self-service user creation flow.
   */
  approve(): void {
    this.isLoading = true;
    this.tenantService.approveFlaggedDocument(this.record.id).subscribe({
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
      // The rejection logic can still use the generic validateRecord endpoint
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