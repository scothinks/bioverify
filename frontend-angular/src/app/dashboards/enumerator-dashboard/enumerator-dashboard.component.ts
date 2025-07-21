import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router'; // <-- Import RouterOutlet

@Component({
  selector: 'app-enumerator-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    RouterOutlet // <-- Add RouterOutlet
  ],
  templateUrl: './enumerator-dashboard.component.html',
})
export class EnumeratorDashboardComponent { }