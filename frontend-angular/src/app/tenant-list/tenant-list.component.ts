
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { Tenant } from '../models/tenant.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../shared/confirm-dialog/confirm-dialog.component';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    CommonModule, MatTableModule, MatButtonModule, MatIconModule, MatCardModule,
    MatSnackBarModule, MatDialogModule, MatProgressSpinnerModule
  ],
  templateUrl: './tenant-list.component.html',
  styleUrls: ['./tenant-list.component.scss']
})
export class TenantListComponent implements OnInit, OnDestroy {
  tenants: Tenant[] = [];
  isLoading = true;
  displayedColumns: string[] = ['name', 'subdomain', 'stateCode', 'createdAt', 'actions'];
  private destroy$ = new Subject<void>();

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.tenantService.tenants$.pipe(takeUntil(this.destroy$)).subscribe(tenants => {
      this.tenants = tenants;
      this.isLoading = false;
    });
    this.tenantService.getTenants().subscribe({
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error loading tenants: ${err.message}`, 'Close', { duration: 5000 });
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  deleteTenant(tenant: Tenant): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Tenant',
        message: `Are you sure you want to delete "${tenant.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tenantService.deleteTenant(tenant.id).subscribe({
          next: () => {
            this.snackBar.open(`Tenant "${tenant.name}" deleted successfully!`, 'Close', { duration: 3000 });
          },
          error: (err) => {
            this.snackBar.open(`Error deleting tenant: ${err.message}`, 'Close', { duration: 5000 });
          }
        });
      }
    });
  }
}