import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VerificationService, OnboardRequest } from '../services/verification.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-Agent-dashboard',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatListModule
  ],
  templateUrl: './Agent-dashboard.component.html',
  styleUrls: ['./Agent-dashboard.component.scss']
})
export class AgentDashboardComponent {
  onboardForm: FormGroup;
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  
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

    const requestData: OnboardRequest = this.onboardForm.value;

    // The entire process is now handled by this single service call
    this.verificationService.onboardUserByAgent(requestData).subscribe({
      next: (response) => {
        this.isLoading = false;
        // Assuming the response contains a success message
        this.successMessage = response.message || 'User has been successfully onboarded and verified!';
        this.onboardForm.reset(); // Reset the form for the next employee
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Onboarding process failed. Please check the details and try again.';
      }
    });
  }

  // The onConfirmDetails method is no longer needed and has been removed.

  // The onLivenessCheck method has been removed to focus on the core onboarding flow.
  // It can be added back as a separate feature in the user dashboard or a different tool.

  reset(): void {
    this.onboardForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
  }
}