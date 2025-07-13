import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantListComponent } from './tenant-list/tenant-list.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { LoginComponent } from './login/login.component';
import { AuthService } from './services/auth.service';
import { TenantFormComponent } from './tenant-form/tenant-form.component';
import { VerificationComponent } from './verification/verification.component'; // <-- IMPORT ADDED

// Material imports for the header
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    TenantListComponent,
    FileUploadComponent,
    LoginComponent,
    TenantFormComponent,
    VerificationComponent, // <-- ADDED TO IMPORTS ARRAY
    MatToolbarModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  isLoggedIn = false;
  userRole: string | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      this.userRole = this.authService.getUserRole();
    }
  }

  logout(): void {
    this.authService.logout();
  }
}