import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { Tenant } from '../models/tenant.model';
import { Subject, takeUntil } from 'rxjs';
import { ConfirmDialogComponent } from '../shared/confirm-dialog/confirm-dialog.component';
import { TenantFormComponent } from '../tenant-form/tenant-form.component';
import { UserManagementComponent } from '../user-management/user-management.component';

// Material imports
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';


@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    CommonModule, MatTableModule, MatButtonModule, MatIconModule, MatCardModule,
    MatSnackBarModule, MatDialogModule, MatProgressSpinnerModule, MatPaginatorModule,
    MatTooltipModule, DatePipe,
    TenantFormComponent, UserManagementComponent
  ],
  templateUrl: './tenant-list.component.html',
  styleUrls: ['./tenant-list.component.scss']
})
export class TenantListComponent implements OnInit, OnDestroy, AfterViewInit {
  dataSource = new MatTableDataSource<any>();
  isLoading = true;
  displayedColumns: string[] = ['name', 'subdomain', 'stateCode', 'validationUrl', 'createdAt', 'actions'];
  private destroy$ = new Subject<void>();

  isManagingUsers = false;
  tenantForUserManagement: Tenant | null = null;
  showForm = false;
  selectedTenant: Tenant | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.tenantService.tenants$.pipe(takeUntil(this.destroy$)).subscribe(tenants => {
      this.dataSource.data = tenants.map(tenant => this.mapTenantData(tenant));
      this.isLoading = false;
    });
    this.loadTenants();
  }
  
  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private mapTenantData(tenant: Tenant): any {
    try {
      if (tenant.identitySourceConfig) {
        const config = JSON.parse(tenant.identitySourceConfig);
        return { ...tenant, validationUrl: config.validationUrl || 'Not Set' };
      }
    } catch {
      return { ...tenant, validationUrl: 'Invalid Config' };
    }
    return { ...tenant, validationUrl: 'Not Set' };
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
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tenantService.deleteTenant(tenant.id).subscribe({
          next: () => {
            this.snackBar.open(`Tenant "${tenant.name}" deleted successfully!`, 'Close', { duration: 3000, panelClass: 'success-snackbar' });
          },
          error: (err) => {
            this.snackBar.open(`Error deleting tenant: ${err.message}`, 'Close', { duration: 5000, panelClass: 'error-snackbar' });
          }
        });
      }
    });
  }
}