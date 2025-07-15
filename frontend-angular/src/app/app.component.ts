import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router'; 
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from './services/auth.service';
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { MasterListComponent } from './master-list/master-list.component';
import { UserDashboardComponent } from './user-dashboard/user-dashboard.component';

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
    MasterListComponent
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoggedIn = false;
  userRole: string | null = null;

  constructor(private authService: AuthService) {}

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
}