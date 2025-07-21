import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router'; // <-- Import RouterOutlet

@Component({
  selector: 'app-self-service-user-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    RouterOutlet // <-- Add RouterOutlet and remove UserDashboardComponent
  ],
  templateUrl: './self-service-user-dashboard.component.html',
})
export class SelfServiceUserDashboardComponent { }