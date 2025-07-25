import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, Observable } from 'rxjs';
import { TenantService, ReviewerData } from '../services/tenant.service';
import { Department } from '../models/department.model';
import { Ministry } from '../models/ministry.model';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { EditAssignmentsDialogComponent } from '../edit-assignment/edit-assignments-dialog.component';

@Component({
  selector: 'app-reviewer-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule,
    MatDialogModule
  ],
  templateUrl: './reviewer-management.component.html',
  styleUrls: ['./reviewer-management.component.scss']
})
export class ReviewerManagementComponent implements OnInit {
  isLoading = true;
  dataSource = new MatTableDataSource<ReviewerData>();
  displayedColumns: string[] = ['fullName', 'pendingValidationCount', 'assignedMinistries', 'assignedDepartments', 'actions'];
  
  allMinistries: Ministry[] = [];
  allDepartments: Department[] = [];

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    if (paginator) {
      this.dataSource.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.isLoading = true;
    forkJoin({
      reviewers: this.tenantService.getReviewers(),
      ministries: this.tenantService.getMinistries(),
      departments: this.tenantService.getDepartments()
    }).subscribe({
      next: ({ reviewers, ministries, departments }) => {
        this.dataSource.data = reviewers;
        this.allMinistries = ministries;
        this.allDepartments = departments;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.showSnackBar('Failed to load reviewer data.', 'error');
        console.error(err);
      }
    });
  }

  openEditDialog(reviewer: ReviewerData): void {
    const dialogRef = this.dialog.open(EditAssignmentsDialogComponent, {
      width: '600px',
      data: {
        reviewer,
        allMinistries: this.allMinistries,
        allDepartments: this.allDepartments
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tenantService.updateReviewerAssignments(reviewer.id, result).subscribe({
          next: () => {
            this.showSnackBar('Reviewer assignments updated successfully.', 'success');
            this.loadInitialData(); // Refresh the data
          },
          error: (err) => {
            this.showSnackBar('Failed to update assignments.', 'error');
            console.error(err);
          }
        });
      }
    });
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  private showSnackBar(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: [`snackbar-${type}`]
    });
  }
}