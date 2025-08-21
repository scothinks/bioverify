import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TenantService } from '../services/tenant.service';
import { MasterListRecord } from '../models/master-list-record.model';
import { RecordEditFormComponent } from '../record-edit-form/record-edit-form.component';
import { RecordMismatchDialogComponent } from '../record-mismatch-dialog/record-mismatch-dialog.component';

// Import necessary Angular Material modules
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-review-queue',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatTableModule,
    MatIconModule, MatButtonModule, MatTooltipModule, MatDialogModule,
    MatSnackBarModule, MatTabsModule, MatPaginatorModule, MatFormFieldModule,
    MatSelectModule, MatInputModule
  ],
  templateUrl: './review-queue.component.html',
  styleUrl: './review-queue.component.scss'
})
export class ReviewQueueComponent implements OnInit {
  
  public awaitingReviewRecords = new MatTableDataSource<MasterListRecord>();
  public mismatchedRecords = new MatTableDataSource<MasterListRecord>();
  
  public displayedColumns: string[] = [
    'fullName', 'department', 'ministry', 'gradeLevel',
    'salaryStructure', 'actions'
  ];
  public selectedTabIndex = 0;

  public filterForm: FormGroup;
  public ministryList: string[] = [];
  public departmentList: string[] = [];
  public gradeList: string[] = [];

  @ViewChild('awaitingReviewPaginator') set awaitingReviewPaginator(paginator: MatPaginator) {
    if (paginator) {
      this.awaitingReviewRecords.paginator = paginator;
    }
  }

  @ViewChild('mismatchedPaginator') set mismatchedPaginator(paginator: MatPaginator) {
    if (paginator) {
      this.mismatchedRecords.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.filterForm = this.fb.group({
      ministry: [[]],
      department: [[]],
      gradeLevel: [[]]
    });
  }

  ngOnInit(): void {
    this.loadDataForCurrentTab();
    this.setupFiltering();
  }

  loadDataForCurrentTab(): void {
    const serviceCall = this.selectedTabIndex === 0 
      ? this.tenantService.getAwaitingReviewQueue() 
      : this.tenantService.getMismatchedQueue();

    serviceCall.subscribe({
      next: (data) => {
        const dataSource = this.selectedTabIndex === 0 ? this.awaitingReviewRecords : this.mismatchedRecords;
        dataSource.data = data;
        this.populateFilterLists(data);
        this.applyFilter();
      },
      error: (error) => this.showSnackBar(`Failed to load queue`, 'error')
    });
  }

  private setupFiltering(): void {
    const customFilterPredicate = (data: MasterListRecord, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);
      
      const ministryMatch = searchTerms.ministry.length > 0 ? data.ministry && searchTerms.ministry.includes(data.ministry) : true;
      const departmentMatch = searchTerms.department.length > 0 ? data.department && searchTerms.department.includes(data.department) : true;
      const gradeLevelMatch = searchTerms.gradeLevel.length > 0 ? data.gradeLevel && searchTerms.gradeLevel.includes(data.gradeLevel) : true;
      
      return ministryMatch && departmentMatch && gradeLevelMatch;
    };

    this.awaitingReviewRecords.filterPredicate = customFilterPredicate;
    this.mismatchedRecords.filterPredicate = customFilterPredicate;

    this.filterForm.valueChanges.subscribe(() => {
      this.applyFilter();
    });
  }
  
  private applyFilter(): void {
    const filterValue = JSON.stringify(this.filterForm.value);
    const dataSource = this.selectedTabIndex === 0 ? this.awaitingReviewRecords : this.mismatchedRecords;
    dataSource.filter = filterValue;
  }

  private populateFilterLists(records: MasterListRecord[]): void {
    if (records.length > 0) {
      this.ministryList = [...new Set(records.map(r => r.ministry).filter((v): v is string => !!v))].sort();
      this.departmentList = [...new Set(records.map(r => r.department).filter((v): v is string => !!v))].sort();
      this.gradeList = [...new Set(records.map(r => r.gradeLevel).filter((v): v is string => !!v))].sort();
    }
  }

  public clearFilters(): void {
    this.filterForm.reset({ ministry: [], department: [], gradeLevel: [] });
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    this.clearFilters();
    this.loadDataForCurrentTab();
  }

  onApprove(recordId: string): void {
    this.tenantService.validateRecord(recordId, 'REVIEWED', 'Approved by reviewer.').subscribe({
      next: () => {
        this.showSnackBar('Record approved successfully!', 'success');
        this.loadDataForCurrentTab();
      },
      error: (error) => this.showSnackBar('Failed to approve record', 'error')
    });
  }

  onReject(recordId: string): void {
    const reason = prompt('Please provide a reason for rejection:');
    if (reason?.trim()) {
      this.tenantService.validateRecord(recordId, 'REJECTED', reason).subscribe({
        next: () => {
          this.showSnackBar('Record rejected successfully!', 'success');
          this.loadDataForCurrentTab();
        },
        error: (error) => this.showSnackBar('Failed to reject record', 'error')
      });
    }
  }

  onEdit(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordEditFormComponent, {
      width: '600px',
      data: record
    });

    dialogRef.afterClosed().subscribe(wasUpdated => {
      if (wasUpdated) {
        this.showSnackBar('Record updated successfully!', 'success');
        this.loadDataForCurrentTab();
      }
    });
  }

  onResolve(record: MasterListRecord): void {
    const dialogRef = this.dialog.open(RecordMismatchDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      data: record
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === 'accept') {
        this.tenantService.resolveMismatch(record.id).subscribe({
          next: () => {
            this.showSnackBar('Record resolved and sent for review!', 'success');
            this.loadDataForCurrentTab();
          },
          error: (err) => this.showSnackBar('Failed to resolve record.', 'error')
        });
      } else if (result === 'edit') {
        this.onEdit(record);
      }
    });
  }

  // ADDED: The missing helper function
  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) {
      return names[0].charAt(0).toUpperCase();
    }
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }
  
  private showSnackBar(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    const config = { 
      duration: 4000, 
      horizontalPosition: 'end' as const, 
      verticalPosition: 'top' as const, 
      panelClass: [`snackbar-${type}`] 
    };
    this.snackBar.open(message, 'Close', config);
  }
}