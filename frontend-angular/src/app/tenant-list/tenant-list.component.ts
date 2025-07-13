import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { Tenant } from '../models/tenant.model';
import { Subject, takeUntil } from 'rxjs';
import { ConfirmDialogComponent } from '../shared/confirm-dialog/confirm-dialog.component';
import { TenantFormComponent } from '../tenant-form/tenant-form.component';
import { UserManagementComponent } from '../user-management/user-management.component'; // <-- IMPORT

// Material imports
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
    MatSnackBarModule, MatDialogModule, MatProgressSpinnerModule,
    TenantFormComponent,
    UserManagementComponent // <-- ADD
  ],
  templateUrl: './tenant-list.component.html',
  styleUrls: ['./tenant-list.component.scss']
})
export class TenantListComponent implements OnInit, OnDestroy {
  tenants: any[] = [];
  isLoading = true;
  displayedColumns: string[] = ['name', 'subdomain', 'stateCode', 'validationUrl', 'createdAt', 'actions'];
  private destroy$ = new Subject<void>();

  // NEW: Properties to manage which view is shown
  isManagingUsers = false;
  tenantForUserManagement: Tenant | null = null;
  showForm = false;
  selectedTenant: Tenant | null = null;

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.tenantService.tenants$.pipe(takeUntil(this.destroy$)).subscribe(tenants => {
      this.tenants = tenants.map(tenant => {
        try {
          if (tenant.identitySourceConfig) {
            const config = JSON.parse(tenant.identitySourceConfig);
            return { ...tenant, validationUrl: config.validationUrl || 'Not Set' };
          }
          return { ...tenant, validationUrl: 'Not Set' };
        } catch {
          return { ...tenant, validationUrl: 'Invalid Config' };
        }
      });
      this.isLoading = false;
    });
    this.loadTenants();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTenants(): void {
    this.isLoading = true;
    this.tenantService.getTenants().subscribe({
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Error loading tenants: ${err.message}`, 'Close', { duration: 5000 });
      }
    });
  }

  // --- Methods to control the create/edit form visibility ---
  openCreateForm(): void {
    this.selectedTenant = null;
    this.showForm = true;
  }

  openEditForm(tenant: Tenant): void {
    this.selectedTenant = tenant;
    this.showForm = true;
  }

  onFormCancelled(): void {
    this.showForm = false;
    this.selectedTenant = null;
  }

  onTenantSaved(tenant: Tenant): void {
    this.showForm = false;
    this.selectedTenant = null;
  }

  // --- NEW: Methods to control the user management view ---
  manageTenantUsers(tenant: Tenant): void {
    this.tenantForUserManagement = tenant;
    this.isManagingUsers = true;
  }

  onCloseUserManagement(): void {
    this.isManagingUsers = false;
    this.tenantForUserManagement = null;
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