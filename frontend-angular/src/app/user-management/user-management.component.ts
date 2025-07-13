// FILE: src/app/user-management/user-management.component.ts

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Tenant } from '../models/tenant.model';
import { AuthService } from '../services/auth.service';

// Material Imports
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatSnackBarModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  @Input() tenant!: Tenant | null;
  @Output() close = new EventEmitter<void>();

  userForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.userForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid || !this.tenant) {
      return;
    }

    const request = {
      ...this.userForm.value,
      role: 'TENANT_ADMIN', // Hardcoded for this form
      tenantId: this.tenant.id
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.snackBar.open('Tenant Admin created successfully!', 'Close', { duration: 3000 });
        this.userForm.reset();
        // In a real app, we would update a list of users here.
      },
      error: (err) => {
        this.snackBar.open(`Error creating user: ${err.message}`, 'Close', { duration: 5000 });
      }
    });
  }
}