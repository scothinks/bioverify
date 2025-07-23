import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-global-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    RouterOutlet 
  ],
  templateUrl: './global-admin-dashboard.component.html',
})
export class GlobalAdminDashboardComponent { }