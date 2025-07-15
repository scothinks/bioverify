import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VerificationService, VerificationResponse } from '../services/verification.service';

// Import Angular Material modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-verification',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatStepperModule, MatProgressSpinnerModule,
    MatIconModule, MatListModule
  ],
  templateUrl: './verification.component.html',
  styleUrls: ['./verification.component.scss']
})
export class VerificationComponent {
  verificationForm: FormGroup;
  isLoading = false;
  isConfirming = false; // To show a spinner on the confirm button
  errorMessage = '';
  successMessage = '';
  verificationResult: VerificationResponse | null = null;

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
    if (this.verificationForm.invalid) return;
    
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.verificationResult = null;

    this.verificationService.verifyIdentity(this.verificationForm.value).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.verificationResult = response;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'An unknown error occurred.';
      }
    });
  }

  onConfirmDetails(): void {
    if (!this.verificationResult?.record?.id) return;

    this.isConfirming = true;
    this.errorMessage = '';
    
    this.verificationService.confirmVerification(this.verificationResult.record.id).subscribe({
      next: (response) => {
        this.isConfirming = false;
        this.successMessage = response.message || 'Details confirmed! Your record is now fully verified.';
        this.verificationResult = null; // Hide the confirmation view
      },
      error: (err) => {
        this.isConfirming = false;
        this.errorMessage = err.error?.message || 'Could not confirm details.';
      }
    });
  }

  onRejectDetails(): void {
    alert('Thank you for your feedback. A support ticket has been raised to correct your details.');
    this.resetForm();
  }

  resetForm(): void {
    this.verificationResult = null;
    this.verificationForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }
}