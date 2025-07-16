import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from './services/auth.service';
import { TenantService } from './services/tenant.service';
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { MasterListComponent } from './master-list/master-list.component';
import { UserDashboardComponent } from './user-dashboard/user-dashboard.component';
import { UserFormComponent } from './user-form/user-form.component';
import { UserListComponent } from './user-list/user-list.component';
import { AgentDashboardComponent } from './agent-dashboard/agent-dashboard.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    TenantListComponent,
    FileUploadComponent,
    UserDashboardComponent,
    MasterListComponent,
    MatDialogModule,
    UserListComponent,
    AgentDashboardComponent // <-- COMPONENT ADDED
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoggedIn = false;
  userRole: string | null = null;

  constructor(
    private authService: AuthService,
    private dialog: MatDialog,
    private tenantService: TenantService
  ) {}

  ngOnInit(): void {
    this.authService.isLoggedIn$.subscribe((status: boolean) => {
      this.isLoggedIn = status;
      if (status) {
        this.userRole = this.authService.getUserRole();
      } else {
        this.userRole = null;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

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
            // Here you could refresh the user list
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