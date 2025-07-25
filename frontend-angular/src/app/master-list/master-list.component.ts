import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

// Add new modules for filter controls
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-master-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule, // Add ReactiveFormsModule
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatFormFieldModule, // Add Material Form Field Module
    MatSelectModule,    // Add Material Select Module
    MatInputModule      // Add Material Input Module
  ],
  templateUrl: './master-list.component.html',
  styleUrls: ['./master-list.component.scss']
})
export class MasterListComponent implements OnInit {
  
  public displayedColumns: string[] = [
    'fullName', 'department', 'ministry', 'gradeLevel', 'status'
  ];
  
  dataSource = new MatTableDataSource<MasterListRecord>();
  public isLoading = true;

  // --- NEW PROPERTIES FOR FILTERING ---
  public filterForm: FormGroup;
  public ministryList: string[] = [];
  public departmentList: string[] = [];
  public gradeList: string[] = [];
  public statusList: string[] = [];

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    if (paginator) {
      this.dataSource.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder // Inject FormBuilder
  ) {
    // Initialize the filter form for multi-select
    this.filterForm = this.fb.group({
      ministry: [[]],
      department: [[]],
      gradeLevel: [[]],
      status: [[]]
    });
  }

  ngOnInit(): void {
    this.loadRecords();
    this.setupFiltering();
  }
  
  // --- UPDATED FILTERING LOGIC ---

  private setupFiltering(): void {
    // Custom predicate for multi-select filtering
    this.dataSource.filterPredicate = (data: MasterListRecord, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);
      
      const ministryMatch = searchTerms.ministry.length > 0 ? data.ministry && searchTerms.ministry.includes(data.ministry) : true;
      const departmentMatch = searchTerms.department.length > 0 ? data.department && searchTerms.department.includes(data.department) : true;
      const gradeLevelMatch = searchTerms.gradeLevel.length > 0 ? data.gradeLevel && searchTerms.gradeLevel.includes(data.gradeLevel) : true;
      const statusMatch = searchTerms.status.length > 0 ? data.status && searchTerms.status.includes(data.status) : true;
      
      return ministryMatch && departmentMatch && gradeLevelMatch && statusMatch;
    };

    // Subscribe to form changes to apply the filter
    this.filterForm.valueChanges.subscribe(() => {
      this.dataSource.filter = JSON.stringify(this.filterForm.value);
    });
  }

  loadRecords(): void {
    this.isLoading = true;
    this.tenantService.getRecordsForTenant().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.populateFilterLists(data); // Populate dropdowns
        this.showSnackBar(`Loaded ${data.length} records successfully`, 'success');
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading records:', error);
        this.showSnackBar('Failed to load records', 'error');
        this.dataSource.data = [];
        this.isLoading = false;
      }
    });
  }

  private populateFilterLists(records: MasterListRecord[]): void {
    if (records.length > 0) {
      this.ministryList = [...new Set(records.map(r => r.ministry).filter((v): v is string => !!v))].sort();
      this.departmentList = [...new Set(records.map(r => r.department).filter((v): v is string => !!v))].sort();
      this.gradeList = [...new Set(records.map(r => r.gradeLevel).filter((v): v is string => !!v))].sort();
      // FIXED: Removed the invalid type guard for the 'status' property.
      this.statusList = [...new Set(records.map(r => r.status).filter(v => !!v))].sort();
    }
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
    this.filterForm.reset({ ministry: [], department: [], gradeLevel: [], status: [] });
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