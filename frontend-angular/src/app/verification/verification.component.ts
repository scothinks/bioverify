import { Component, Input } from '@angular/core'; // Import Input
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VerificationService, VerificationRequest, VerificationResponse } from '../services/verification.service';

// Import Angular Material modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-verification',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule,
    MatIconModule, MatListModule
  ],
  templateUrl: './verification.component.html',
  styleUrls: ['./verification.component.scss']
})
export class VerificationComponent {
  // This component now requires a recordId to know what to verify.
  @Input() recordId: string | null = null;

  verificationForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private verificationService: VerificationService
  ) {
    this.verificationForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.verificationForm.invalid || !this.recordId) {
      this.errorMessage = 'Record ID is missing or form is invalid.';
      return;
    }
    
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: VerificationRequest = this.verificationForm.value;

    // Call the updated service method with both recordId and data
    this.verificationService.verifyIdentity(this.recordId, request).subscribe({
      next: (response: VerificationResponse) => {
        this.isLoading = false;
        this.successMessage = `Verification submitted. New Status: ${response.newStatus}`;
        this.verificationForm.disable(); // Disable form on success
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unknown error occurred.';
      }
    });
  }

  // The onConfirmDetails and onRejectDetails methods are obsolete and have been removed.

  resetForm(): void {
    this.verificationForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }
}