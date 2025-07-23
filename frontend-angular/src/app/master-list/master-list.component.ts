import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
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
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './master-list.component.html',
  styleUrls: ['./master-list.component.scss']
})
export class MasterListComponent implements OnInit, OnDestroy { // Removed AfterViewInit
  
  public displayedColumns: string[] = [
    'fullName', 'department', 'ministry', 'gradeLevel', 'status'
  ];
  
  dataSource = new MatTableDataSource<MasterListRecord>();
  public allRecords: MasterListRecord[] = [];
  private filterSubscription!: Subscription;
  public isLoading = true;

  // --- UPDATED PAGINATOR LOGIC ---
  // Use a setter to ensure the paginator is assigned to the datasource
  // as soon as it becomes available in the view, fixing timing issues with *ngIf.
  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    if (paginator) {
      this.dataSource.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadRecords();
    this.setupFilterSubscription();
  }
  
  // The ngAfterViewInit hook is no longer needed and has been removed.

  ngOnDestroy(): void {
    if (this.filterSubscription) {
      this.filterSubscription.unsubscribe();
    }
  }

  private setupFilterSubscription(): void {
    this.filterSubscription = this.tenantService.recordsFilter$.subscribe({
      next: (recordIds) => {
        let filteredData: MasterListRecord[];
        if (recordIds && recordIds.length > 0) {
          filteredData = this.allRecords.filter(record => recordIds.includes(record.id));
          this.showSnackBar(`Filtered to ${filteredData.length} records`, 'info');
        } else {
          filteredData = this.allRecords;
        }
        this.dataSource.data = filteredData;
      }
    });
  }

  loadRecords(): void {
    this.isLoading = true;
    this.tenantService.getRecordsForTenant().subscribe({
      next: (data) => {
        this.allRecords = data;
        this.dataSource.data = data;
        this.showSnackBar(`Loaded ${data.length} records successfully`, 'success');
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading records:', error);
        this.showSnackBar('Failed to load records', 'error');
        this.allRecords = [];
        this.dataSource.data = [];
        this.isLoading = false;
      }
    });
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  getStatusClass(status: string): string {
    return status?.toLowerCase() || 'unknown';
  }

  getStatusIcon(status: string): string {
    switch (status?.toLowerCase()) {
      case 'validated': return 'check_circle';
      case 'rejected': return 'cancel';
      default: return 'help';
    }
  }

  clearFilters(): void {
    this.dataSource.data = this.allRecords;
    this.showSnackBar('Filters cleared', 'info');
  }
  
  onUploadClick(): void {
    this.showSnackBar('Upload functionality would be implemented here', 'info');
  }

  private showSnackBar(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: [`snackbar-${type}`]
    });
  }
}