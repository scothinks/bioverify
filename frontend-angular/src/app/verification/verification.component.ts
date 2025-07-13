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
  errorMessage = '';
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
    if (this.verificationForm.invalid) {
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
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
    // In a real app, this would call another service to finalize the record.
    // For now, we just show a success message.
    alert('Details confirmed! Your record is now fully verified.');
    this.verificationResult = null; // Reset the view
    this.verificationForm.reset();
  }

  onRejectDetails(): void {
    // Logic to handle when a user says the details are incorrect.
    alert('Thank you for your feedback. A support ticket has been raised to correct your details.');
    this.verificationResult = null; // Reset the view
    this.verificationForm.reset();
  }
}