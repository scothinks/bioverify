// src/app/employee-registration/employee-registration.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService, AuthRequest } from '../services/auth.service';
import { VerificationService, VerifyIdentityRequest, PsnChallengeRequest } from '../services/verification.service';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-employee-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    RouterModule
  ],
  templateUrl: './employee-registration.component.html',
  styleUrls: ['./employee-registration.component.scss']
})
export class EmployeeRegistrationComponent implements OnInit {
  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  currentStep: 'initial' | 'challenge' | 'create' = 'initial';
  isLoggedIn = false; // Property to track login state for the view
  
  private recordId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private verificationService: VerificationService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required],
      psn: ['', Validators.required], // Added for the challenge step
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit(): void {
    // Check auth status when the component loads
    this.isLoggedIn = this.authService.isLoggedIn();
  }

  // Step 1: Initiate the verification process
  onInitiateVerification(): void {
    if (this.registerForm.get('ssid')?.invalid || this.registerForm.get('nin')?.invalid) return;
    
    this.isLoading = true;
    this.errorMessage = '';
    
    const request: VerifyIdentityRequest = {
      ssid: this.registerForm.value.ssid,
      nin: this.registerForm.value.nin
    };

    this.verificationService.initiateVerification(request).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.recordId = response.recordId; // Store the recordId for subsequent steps

        if (response.nextStep === 'CHALLENGE_PSN') {
          this.currentStep = 'challenge';
        } else if (response.nextStep === 'CREATE_ACCOUNT') {
          this.currentStep = 'create';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'Verification failed. Please check your SSID and NIN.';
      }
    });
  }

  // Step 2 (Conditional): Handle the PSN challenge
  onChallenge(): void {
    if (this.registerForm.get('psn')?.invalid || !this.recordId) return;

    this.isLoading = true;
    this.errorMessage = '';
    
    const request: PsnChallengeRequest = {
      recordId: this.recordId,
      psn: this.registerForm.value.psn
    };

    this.verificationService.resolvePsnChallenge(request).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.currentStep = 'create'; // Move to the final step
        } else {
          this.errorMessage = response.message || 'An unexpected error occurred.';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'The provided PSN is incorrect.';
      }
    });
  }

  // Step 3: Handle Account Creation
  onCreateAccount(): void {
    if (this.registerForm.get('email')?.invalid || this.registerForm.get('password')?.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const accountData: AuthRequest = {
        email: this.registerForm.value.email,
        password: this.registerForm.value.password,
        recordId: this.recordId! 
    };

    this.authService.createAccount(accountData).subscribe({
        next: () => {
            this.isLoading = false;
            this.successMessage = 'Success! Your account has been created. Redirecting to login...';
            setTimeout(() => this.router.navigate(['/login']), 2000);
        },
        error: (err) => {
            this.isLoading = false;
            this.errorMessage = err.message || 'Account creation failed. Please try again or contact support.';
        }
    });
  }
}