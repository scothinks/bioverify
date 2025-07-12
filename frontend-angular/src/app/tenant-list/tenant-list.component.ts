// FILE: src/app/tenant-list/tenant-list.component.ts

import { Component, OnInit } from '@angular/core';
import { TenantService } from '../services/tenant.service';
import { Tenant } from '../models/tenant.model';
import { CommonModule } from '@angular/common';

// Import Angular Material modules for the table
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';


@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './tenant-list.component.html',
  styleUrl: './tenant-list.component.scss'
})
export class TenantListComponent implements OnInit {

  // A property to hold our list of tenants.
  public tenants: Tenant[] = [];
  // A flag to show a loading spinner while we fetch data.
  public isLoading = true;
  // Define the columns to be displayed in the table.
  public displayedColumns: string[] = ['name', 'subdomain', 'stateCode', 'active'];

  // Inject our TenantService.
  constructor(private tenantService: TenantService) {}

  // This method runs automatically when the component is first loaded.
  ngOnInit(): void {
    this.tenantService.getTenants().subscribe({
      next: (data) => {
        this.tenants = data; // Assign the fetched data to our property.
        this.isLoading = false; // Hide the loading spinner.
      },
      error: (err) => {
        console.error('Failed to fetch tenants', err);
        this.isLoading = false; // Hide the spinner even if there's an error.
      }
    });
  }
}