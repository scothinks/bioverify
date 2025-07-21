import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

@Component({
  selector: 'app-master-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './master-list.component.html',
  styleUrls: ['./master-list.component.scss']
})
export class MasterListComponent implements OnInit, OnDestroy {
  
  allRecords: MasterListRecord[] = [];
  filteredRecords: MasterListRecord[] = [];
  public displayedColumns: string[] = [
    'id',
    'fullName', 
    'department', 
    'ministry', 
    'gradeLevel', 
    'salaryStructure', 
    'status'
  ];
  
  private filterSubscription!: Subscription;
  private loadingTimeout?: number;

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadRecords();
    this.setupFilterSubscription();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions to prevent memory leaks
    if (this.filterSubscription) {
      this.filterSubscription.unsubscribe();
    }
    
    if (this.loadingTimeout) {
      clearTimeout(this.loadingTimeout);
    }
  }

  private setupFilterSubscription(): void {
    // Listen for filter requests from the service
    this.filterSubscription = this.tenantService.recordsFilter$.subscribe({
      next: (recordIds) => {
        if (recordIds && recordIds.length > 0) {
          // If a list of IDs is provided, filter the records
          this.filteredRecords = this.allRecords.filter(record => 
            recordIds.includes(record.id)
          );
          this.showSnackBar(`Filtered to ${this.filteredRecords.length} records`, 'info');
        } else {
          // If the filter is cleared (null), show all records
          this.filteredRecords = this.allRecords;
        }
      },
      error: (error) => {
        console.error('Error in filter subscription:', error);
        this.showSnackBar('Filter update failed', 'error');
      }
    });
  }

  loadRecords(): void {
    // Add a small delay to show loading state for better UX
    this.loadingTimeout = window.setTimeout(() => {
      this.tenantService.getRecordsForTenant().subscribe({
        next: (data) => {
          this.allRecords = data;
          this.filteredRecords = data; // Initially, show all records
          this.showSnackBar(`Loaded ${data.length} records successfully`, 'success');
        },
        error: (error) => {
          console.error('Error loading records:', error);
          this.showSnackBar('Failed to load records', 'error');
          // Set empty arrays on error to show empty state
          this.allRecords = [];
          this.filteredRecords = [];
        }
      });
    }, 500); // Small delay for better perceived performance
  }

  // Helper method to get employee initials for avatar
  getInitials(fullName: string): string {
    if (!fullName) return '?';
    
    const names = fullName.trim().split(' ');
    if (names.length === 1) {
      return names[0].charAt(0).toUpperCase();
    }
    
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  // Helper method to get status-specific CSS class
  getStatusClass(status: string): string {
    if (!status) return 'unknown';
    
    const statusLower = status.toLowerCase();
    
    switch (statusLower) {
      case 'pending':
        return 'pending';
      case 'approved':
      case 'validated':
        return 'validated';
      case 'rejected':
        return 'rejected';
      default:
        return 'unknown';
    }
  }

  // Helper method to get status-specific icon
  getStatusIcon(status: string): string {
    if (!status) return 'help';
    
    const statusLower = status.toLowerCase();
    
    switch (statusLower) {
      case 'pending':
        return 'schedule';
      case 'approved':
      case 'validated':
        return 'check_circle';
      case 'rejected':
        return 'cancel';
      default:
        return 'help';
    }
  }

  // Helper method to get filtered count for header stats
  getFilteredCount(): number {
    return this.filteredRecords?.length || 0;
  }

  // Method to handle upload button click
  onUploadClick(): void {
    // This would typically open a file upload dialog or navigate to upload page
    // For now, show a placeholder message
    this.showSnackBar('Upload functionality would be implemented here', 'info');
    
    // Example of how you might implement this:
    // this.router.navigate(['/upload']);
    // or
    // this.fileUploadService.openUploadDialog();
  }

  // Enhanced snackbar method with styling
  private showSnackBar(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const config = {
      duration: type === 'error' ? 6000 : 4000,
      horizontalPosition: 'end' as const,
      verticalPosition: 'top' as const,
      panelClass: [`snackbar-${type}`]
    };

    this.snackBar.open(message, 'Close', config);
  }

  // Method to refresh records manually
  refreshRecords(): void {
    this.loadRecords();
  }

  // Method to clear any active filters
  clearFilters(): void {
    this.filteredRecords = this.allRecords;
    this.showSnackBar('Filters cleared', 'info');
  }

  // Method to get record count by status
  getStatusCount(status: string): number {
    return this.allRecords.filter(record => 
      record.status?.toLowerCase() === status.toLowerCase()
    ).length;
  }

  // Method to export records (placeholder)
  exportRecords(): void {
    // This would implement CSV/Excel export functionality
    this.showSnackBar('Export functionality would be implemented here', 'info');
  }
}