import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { Observable } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';

// Define a simple user interface here or import from a model file
export interface User {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatCardModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  
  public users$!: Observable<User[]>;
  displayedColumns: string[] = ['fullName', 'email', 'role'];

  constructor(private tenantService: TenantService) {}

  ngOnInit(): void {
    this.users$ = this.tenantService.getUsers();
  }
}