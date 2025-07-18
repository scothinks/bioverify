import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Tenant } from '../models/tenant.model';
import { AuthService, AuthRequest } from '../services/auth.service'; // Import AuthRequest
import { HttpErrorResponse } from '@angular/common/http'; // Import HttpErrorResponse

// Material Imports
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';


@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  @Input() tenant!: Tenant | null;
  @Output() close = new EventEmitter<void>();

  userForm!: FormGroup;
  hidePassword = true;
  isSubmitting = false;

  readonly roles = [
    { value: 'TENANT_ADMIN', label: 'Administrator' },
    { value: 'TENANT_MANAGER', label: 'Manager' },
    { value: 'TENANT_USER', label: 'User' }
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.userForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['TENANT_ADMIN', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid || !this.tenant) {
      this.markFormGroupTouched(this.userForm);
      return;
    }

    this.isSubmitting = true;
    const formValue = this.userForm.value;

    const request: AuthRequest = {
      fullName: formValue.fullName.trim(),
      email: formValue.email.trim().toLowerCase(),
      password: formValue.password,
      role: formValue.role,
      // Assuming you will modify your backend to accept tenantId in this request
      // tenantId: this.tenant.id 
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.snackBar.open('User created successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.userForm.reset();
        this.userForm.patchValue({ role: 'TENANT_ADMIN' });
        this.hidePassword = true;
        this.isSubmitting = false;
      },
      // Add the correct type to the error parameter
      error: (err: HttpErrorResponse) => {
        this.snackBar.open(this.getErrorMessage(err), 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        this.isSubmitting = false;
      }
    });
  }

  resetForm(): void {
    this.userForm.reset();
    this.userForm.patchValue({ role: 'TENANT_ADMIN' });
    this.hidePassword = true;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error?.error?.message) {
      return error.error.message;
    }
    if (error?.message) {
      return error.message;
    }
    return 'An unexpected error occurred. Please try again.';
  }
}