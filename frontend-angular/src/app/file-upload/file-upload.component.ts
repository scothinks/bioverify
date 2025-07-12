// FILE: src/app/file-upload/file-upload.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { HttpEventType, HttpResponse } from '@angular/common/http';

// Import Angular Material modules
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip'; // Added for tooltips

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatIconModule,
    MatTooltipModule // Added
  ],
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent {
  selectedFile?: File;
  uploadProgress = 0;
  uploadMessage = '';
  isError = false;

  // Hardcoding the Kwara tenant ID for this example.
  // In a real app, this would come from a dropdown or user context.
  kwaraTenantId = '8ac1c21e-9b46-4008-8fef-c9911d966d91';

  constructor(private tenantService: TenantService) {}

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0];
    this.uploadProgress = 0;
    this.uploadMessage = '';
    this.isError = false;
  }

  uploadFile(): void {
    if (!this.selectedFile) {
      return;
    }

    this.uploadProgress = 0;
    this.uploadMessage = '';

    this.tenantService.uploadMasterList(this.selectedFile, this.kwaraTenantId).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.UploadProgress) {
          if (event.total) {
            this.uploadProgress = Math.round(100 * event.loaded / event.total);
          }
        } else if (event instanceof HttpResponse) {
          this.uploadMessage = event.body?.message || 'File uploaded successfully!';
          this.isError = false;
        }
      },
      error: (err: any) => {
        this.uploadProgress = 0;
        console.error('Upload error:', err);
        
        let errorMessage = 'Could not upload the file!';
        
        if (err.error?.message) {
          errorMessage += ' ' + err.error.message;
        } else if (err.message) {
          errorMessage += ' ' + err.message;
        } else if (err.status) {
          errorMessage += ` HTTP ${err.status}`;
          if (err.statusText) {
            errorMessage += ` - ${err.statusText}`;
          }
        }
        
        this.uploadMessage = errorMessage;
        this.isError = true;
      }
    });
  }

  // Helper method to check if upload is in progress
  get isUploading(): boolean {
    return this.uploadProgress > 0 && this.uploadProgress < 100;
  }

  // Helper method to check if file is selected
  get hasFileSelected(): boolean {
    return !!this.selectedFile;
  }

  // Helper method to get file name
  get fileName(): string {
    return this.selectedFile?.name || '';
  }

  // Helper method to format file size
  get fileSize(): string {
    if (!this.selectedFile) return '';
    
    const bytes = this.selectedFile.size;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    
    if (bytes === 0) return '0 Bytes';
    
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
  }

  // Method to reset the upload state
  resetUpload(): void {
    this.selectedFile = undefined;
    this.uploadProgress = 0;
    this.uploadMessage = '';
    this.isError = false;
    
    // Reset the file input
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }
}