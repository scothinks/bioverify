// src/app/user-dashboard/user-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { MasterListRecordDto } from '../services/pol.service'; // Use the DTO
import { LivenessCheckComponent } from '../liveness-check/liveness-check.component';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule,
    MatListModule, MatGridListModule, MatDividerModule, LivenessCheckComponent
  ],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss']
})
export class UserDashboardComponent implements OnInit {

  userRecord: MasterListRecordDto | null = null; // Use the DTO
  isLoading = true;
  statusMessage = '';
  isLivenessCheckDue = false;
  daysRemaining: number | null = null;

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loadUserRecord();
  }

  loadUserRecord(): void {
    this.isLoading = true;
    this.authService.getCurrentUserRecord().subscribe({
      next: (record: MasterListRecordDto) => { // Expect the DTO
        this.userRecord = record;
        this.calculateLivenessCountdown();
        this.isLoading = false;
      },
      error: (err: any) => {
        this.statusMessage = 'Could not load your record. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  calculateLivenessCountdown(): void {
    if (this.userRecord?.nextLivenessCheckDate) {
      const dueDate = new Date(this.userRecord.nextLivenessCheckDate);
      const today = new Date();
      const differenceInTime = dueDate.getTime() - today.getTime();
      const differenceInDays = Math.ceil(differenceInTime / (1000 * 3600 * 24));

      this.daysRemaining = differenceInDays > 0 ? differenceInDays : 0;
      this.isLivenessCheckDue = this.daysRemaining <= 14;
    }
  }

  onLivenessCheckCompleted(): void {
    this.isLivenessCheckDue = false;
    this.statusMessage = "Thank you! Your liveness check has been successfully submitted.";
    this.loadUserRecord();
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }
}