import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { UserFormComponent } from '../../user-form/user-form.component';
import { TenantService } from '../../services/tenant.service';

@Component({
  selector: 'app-tenant-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule
  ],
  templateUrl: './tenant-admin-dashboard.component.html',
})
export class TenantAdminDashboardComponent {
  
  // Navigation links for the tab group
  navLinks = [
    { path: 'users', label: 'User Management', icon: 'group' },
    { path: 'uploads', label: 'File Upload', icon: 'upload_file' },
    { path: 'records', label: 'Master Records', icon: 'list_alt' },
    { path: 'bulk-verify', label: 'Bulk Verification', icon: 'checklist' },
    { path: 'validation', label: 'Validation Queue', icon: 'fact_check' } // <-- ADDED
  ];

  constructor(
    private dialog: MatDialog,
    private tenantService: TenantService
  ) {}

  openCreateUserDialog(): void {
    const dialogRef = this.dialog.open(UserFormComponent, {
      width: '450px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tenantService.createUser(result).subscribe({
          next: (newUser) => {
            console.log('User created successfully:', newUser);
            alert('User created successfully!');
            // To auto-refresh the list, a shared service with a Subject would be needed
          },
          error: (err) => {
            console.error('Failed to create user:', err);
            alert('Error: ' + err.message);
          }
        });
      }
    });
  }
}