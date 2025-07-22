import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { HttpEventType, HttpResponse } from '@angular/common/http';

// Import Angular Material modules
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

// Interface to match the backend DTO
export interface UploadSummary {
  newRecordsCreated: number;
  existingRecordsUpdated: number;
  recordsRequiringReverificationIds: string[];
}

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent {
  selectedFile?: File;
  uploadProgress = 0;
  uploadMessage = '';
  isError = false;

  uploadSummary: UploadSummary | null = null;

  constructor(private tenantService: TenantService) {}

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0];
    this.uploadProgress = 0;
    this.uploadMessage = '';
    this.isError = false;
    this.uploadSummary = null;
  }

  uploadFile(): void {
    if (!this.selectedFile) return;

    this.uploadProgress = 1; // Start progress immediately
    this.uploadMessage = '';
    this.isError = false;
    this.uploadSummary = null;

    this.tenantService.uploadMasterList(this.selectedFile).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.UploadProgress) {
          if (event.total) {
            this.uploadProgress = Math.round(100 * event.loaded / event.total);
          }
        } else if (event instanceof HttpResponse) {
          this.uploadMessage = 'File processed successfully!';
          this.uploadSummary = event.body as UploadSummary;
          this.isError = false;
          this.uploadProgress = 100; // Ensure progress is 100 on completion
        }
      },
      error: (err: any) => {
        this.uploadProgress = 0;
        console.error('Upload error:', err);
        this.uploadMessage = err.error?.message || 'Could not upload the file!';
        this.isError = true;
      }
    });
  }

  onViewRecordsToVerify(): void {
    if (this.uploadSummary?.recordsRequiringReverificationIds) {
      this.tenantService.applyRecordFilter(this.uploadSummary.recordsRequiringReverificationIds);
    }
  }

  onNotifyRecords(): void {
    if (this.uploadSummary?.recordsRequiringReverificationIds && this.uploadSummary.recordsRequiringReverificationIds.length > 0) {
      this.tenantService.notifyForReverification(this.uploadSummary.recordsRequiringReverificationIds)
        .subscribe({
          next: (response) => alert(response.message || 'Notification process started!'),
          error: (err) => alert('Failed to start notification process.')
        });
    }
  }

  get isUploading(): boolean { return this.uploadProgress > 0 && this.uploadProgress < 100; }
  get hasFileSelected(): boolean { return !!this.selectedFile; }
  get fileName(): string { return this.selectedFile?.name || ''; }
  get fileSize(): string {
    if (!this.selectedFile) return '';
    const bytes = this.selectedFile.size;
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  resetUpload(): void {
    this.selectedFile = undefined;
    this.uploadProgress = 0;
    this.uploadMessage = '';
    this.isError = false;
    this.uploadSummary = null;
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }
}