// src/app/user-dashboard/user-dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VerificationService, VerifyIdentityRequest, VerificationResult } from '../services/verification.service';
import { AuthService } from '../services/auth.service';
import { MasterListRecord } from '../models/master-list-record.model';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RecordStatus } from '../models/record-status.enum';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatListModule
  ],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss']
})
export class UserDashboardComponent implements OnInit {

  reverifyForm: FormGroup;
  userRecord: MasterListRecord | null = null;
  isLoading = false;
  statusMessage = '';

  constructor(
    private fb: FormBuilder,
    private verificationService: VerificationService,
    private authService: AuthService
  ) {
    this.reverifyForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.isLoading = true;
    this.authService.getCurrentUserRecord().subscribe({
      next: (record) => {
        this.userRecord = record;
        this.isLoading = false;
      },
      error: (err: any) => {
        this.statusMessage = 'Could not load your record. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  onReverifySubmit(): void {
    if (this.reverifyForm.invalid || !this.userRecord) return;

    this.isLoading = true;
    this.statusMessage = '';

    const request: VerifyIdentityRequest = this.reverifyForm.value;

    this.verificationService.reverifyByIdentity(this.userRecord.id, request).subscribe({
      next: (response: VerificationResult) => {
        this.isLoading = false;
        if (this.userRecord) {
          this.userRecord.status = response.newStatus as RecordStatus;
        }
        this.statusMessage = 'Re-verification successful! Your record is now pending final review.';
        this.reverifyForm.disable();
      },
      error: (err: any) => {
        this.isLoading = false;
        this.statusMessage = 'Re-verification failed. Please check your details and try again.';
      }
    });
  }
}