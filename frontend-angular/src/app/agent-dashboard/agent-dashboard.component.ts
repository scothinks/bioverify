import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VerificationService, VerificationResponse, OnboardRequest } from '../services/verification.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatListModule
  ],
  templateUrl: './agent-dashboard.component.html',
  styleUrls: ['./agent-dashboard.component.scss']
})
export class AgentDashboardComponent {
  onboardForm: FormGroup;
  
  isLoading = false;
  isConfirming = false;
  errorMessage = '';
  successMessage = '';
  
  verificationResult: VerificationResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private verificationService: VerificationService
  ) {
    this.onboardForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onOnboardSubmit(): void {
    if (this.onboardForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.verificationResult = null;

    const requestData: OnboardRequest = this.onboardForm.value;

    this.verificationService.onboardUserByAgent(requestData).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.verificationResult = response;
        } else {
          this.errorMessage = response.message;
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Onboarding process failed.';
      }
    });
  }

  onConfirmDetails(): void {
    if (!this.verificationResult?.record?.id) return;

    this.isConfirming = true;
    this.verificationService.confirmVerification(this.verificationResult.record.id).subscribe({
      next: (response) => {
        this.isConfirming = false;
        this.successMessage = response.message || 'Record has been successfully verified!';
        this.reset();
      },
      error: (err) => {
        this.isConfirming = false;
        this.errorMessage = err.error?.message || 'Could not finalize verification.';
      }
    });
  }

  // --- NEW METHOD for Liveness Check ---
  onLivenessCheck(): void {
    if (!this.verificationResult?.record?.id) return;

    this.isConfirming = true; // Reuse the confirming spinner
    this.verificationService.performLivenessCheck(this.verificationResult.record.id).subscribe({
      next: (response) => {
        this.isConfirming = false;
        // Update the local record to show the new date immediately
        if(this.verificationResult?.record) {
            this.verificationResult.record.lastProofOfLifeDate = new Date().toISOString();
        }
        alert('Proof of Life date updated successfully!');
      },
      error: (err) => {
        this.isConfirming = false;
        this.errorMessage = err.error?.message || 'Liveness check failed.';
      }
    });
  }

  reset(): void {
    this.onboardForm.reset();
    this.verificationResult = null;
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
    this.isConfirming = false;
  }
}