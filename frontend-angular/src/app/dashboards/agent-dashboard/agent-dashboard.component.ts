import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-Agent-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    RouterOutlet 
  ],
  templateUrl: './Agent-dashboard.component.html',
})
export class AgentDashboardComponent { }