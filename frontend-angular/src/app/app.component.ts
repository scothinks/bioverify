// FILE: src/app/app.component.ts (UPDATED)

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from './services/auth.service';
import { LoginComponent } from './login/login.component';
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { VerificationComponent } from './verification/verification.component';
import { MasterListComponent } from './master-list/master-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    LoginComponent,
    TenantListComponent,
    FileUploadComponent,
    VerificationComponent,
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