import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router, RouterLink } from '@angular/router';

// Import Angular Material modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCheckboxModule,
    RouterLink
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  isLoading = false;
  hidePassword = true;
  loginAttempts = 0;
  maxLoginAttempts = 3;
  isBlocked = false;
  blockTimeRemaining = 0;
  private blockTimer?: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    // --- If user is already logged in, redirect them immediately ---
    if (this.authService.isLoggedIn()) {
      this.authService.redirectUserBasedOnRole();
      return;
    }
    
    const savedEmail = localStorage.getItem('rememberedEmail');
    if (savedEmail) {
      this.loginForm.patchValue({ email: savedEmail, rememberMe: true });
    }
    this.checkBlockStatus();
  }

  ngOnDestroy(): void {
    if (this.blockTimer) {
      clearInterval(this.blockTimer);
    }
  }

  get blockMinutes(): number {
    return Math.floor(this.blockTimeRemaining / 60);
  }

  get blockSeconds(): number {
    return this.blockTimeRemaining % 60;
  }

  private checkBlockStatus(): void {
    const blockUntil = localStorage.getItem('loginBlockUntil');
    if (blockUntil) {
      const blockTime = new Date(blockUntil);
      const now = new Date();
      
      if (now < blockTime) {
        this.isBlocked = true;
        this.blockTimeRemaining = Math.ceil((blockTime.getTime() - now.getTime()) / 1000);
        this.startBlockTimer();
      } else {
        localStorage.removeItem('loginBlockUntil');
        localStorage.removeItem('loginAttempts');
        this.loginAttempts = 0;
      }
    }
    const attempts = localStorage.getItem('loginAttempts');
    if (attempts) {
      this.loginAttempts = parseInt(attempts, 10);
    }
  }

  private startBlockTimer(): void {
    this.blockTimer = setInterval(() => {
      this.blockTimeRemaining--;
      if (this.blockTimeRemaining <= 0) {
        this.isBlocked = false;
        this.loginAttempts = 0;
        localStorage.removeItem('loginBlockUntil');
        localStorage.removeItem('loginAttempts');
        clearInterval(this.blockTimer);
        this.showMessage('You can now try logging in again.');
      }
    }, 1000);
  }

  private blockUser(): void {
    const blockDuration = 15 * 60 * 1000; // 15 minutes
    const blockUntil = new Date(Date.now() + blockDuration);
    
    localStorage.setItem('loginBlockUntil', blockUntil.toISOString());
    localStorage.setItem('loginAttempts', this.loginAttempts.toString());
    
    this.isBlocked = true;
    this.blockTimeRemaining = Math.ceil(blockDuration / 1000);
    this.startBlockTimer();
    
    this.showMessage('Too many failed attempts. Please try again in 15 minutes.', 'error');
  }

  private showMessage(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: type === 'error' ? ['error-snackbar'] : ['success-snackbar']
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  getFieldError(fieldName: string): string {
    const field = this.loginForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} is required`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
      }
      if (field.errors['minlength']) {
        return `Password must be at least ${field.errors['minlength'].requiredLength} characters`;
      }
    }
    return '';
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.isLoading || this.isBlocked) {
      return;
    }

    this.isLoading = true;
    const formValue = this.loginForm.value;

    if (formValue.rememberMe) {
      localStorage.setItem('rememberedEmail', formValue.email);
    } else {
      localStorage.removeItem('rememberedEmail');
    }

    this.authService.login({
      email: formValue.email,
      password: formValue.password
    }).subscribe({
      next: (response) => {
        // The success block is now simplified.
        // AuthService handles the redirection automatically.
        this.isLoading = false;
        this.loginAttempts = 0;
        localStorage.removeItem('loginAttempts');
        localStorage.removeItem('loginBlockUntil');
        
        this.showMessage('Login successful! Welcome back.');
      },
      error: (error) => {
        this.isLoading = false;
        this.loginAttempts++;
        
        if (this.loginAttempts >= this.maxLoginAttempts) {
          this.blockUser();
        } else {
          const remainingAttempts = this.maxLoginAttempts - this.loginAttempts;
          this.showMessage(
            `Invalid credentials. ${remainingAttempts} attempt(s) remaining.`,
            'error'
          );
          localStorage.setItem('loginAttempts', this.loginAttempts.toString());
        }
        
        console.error('Authentication failed:', error);
      }
    });
  }
}