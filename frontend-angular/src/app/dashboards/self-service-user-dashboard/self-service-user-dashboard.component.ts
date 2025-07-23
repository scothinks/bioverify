import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router'; 

@Component({
  selector: 'app-self-service-user-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    RouterOutlet 
  ],
  templateUrl: './self-service-user-dashboard.component.html',
})
export class SelfServiceUserDashboardComponent { }