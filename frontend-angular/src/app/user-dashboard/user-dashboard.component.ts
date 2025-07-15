import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { VerificationService } from '../services/verification.service';
import { MasterListRecord } from '../models/master-list-record.model';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';


@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule,
    MatIconModule, MatListModule, MatProgressSpinnerModule
  ],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss']
})
export class UserDashboardComponent implements OnInit {
  
  userRecord: MasterListRecord | null = null;
  isLoading = true;
  isConfirming = false;
  successMessage = '';

  constructor(
    private authService: AuthService,
    private verificationService: VerificationService
  ) {}

  ngOnInit(): void {
    this.loadUserRecord();
  }

  loadUserRecord(): void {
    this.isLoading = true;
    // We need a new service method to get the current user's record
    this.authService.getCurrentUserRecord().subscribe(record => {
      this.userRecord = record;
      this.isLoading = false;
    });
  }

  onConfirmDetails(): void {
    if (!this.userRecord?.id) return;

    this.isConfirming = true;
    this.verificationService.confirmVerification(this.userRecord.id).subscribe({
      next: (response) => {
        this.isConfirming = false;
        this.successMessage = response.message || 'Details confirmed! Your record is now fully verified.';
        // Reload the record to get the updated "VERIFIED" status
        this.loadUserRecord();
      },
      error: (err) => {
        this.isConfirming = false;
        alert(err.error?.message || 'Could not confirm details.');
      }
    });
  }
}