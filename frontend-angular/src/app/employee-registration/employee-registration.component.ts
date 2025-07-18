import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService, AuthRequest } from '../services/auth.service';
import { VerificationService, VerificationRequest, VerificationResponse } from '../services/verification.service';

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
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule
  ],
  templateUrl: './employee-registration.component.html',
  styleUrls: ['./employee-registration.component.scss']
})
export class EmployeeRegistrationComponent {
  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  private recordId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private verificationService: VerificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.registerForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.recordId = this.route.snapshot.queryParams['recordId'];
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;
    if (!this.recordId) {
        this.errorMessage = 'Error: No record identifier found. Please use the link provided in your email.';
        return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formValues = this.registerForm.value;
    const verificationRequest: VerificationRequest = {
      ssid: formValues.ssid,
      nin: formValues.nin
    };

    this.verificationService.verifyIdentity(this.recordId, verificationRequest).subscribe({
      next: (verificationResponse) => {
        if (verificationResponse.newStatus === 'PENDING_GRADE_VALIDATION') {
          this.createAccount(formValues.email, formValues.password);
        } else {
          this.isLoading = false;
          this.errorMessage = 'Identity verified, but your record requires review. Please contact support.';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'Identity Verification Failed. Please check your SSID and NIN.';
      }
    });
  }

  private createAccount(email: string, password: string): void {
    const accountData: AuthRequest = {
        email,
        password,
        // The '!' tells TypeScript that we guarantee recordId is not null here.
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