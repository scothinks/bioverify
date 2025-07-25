import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon'; // <-- Import MatIconModule
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

@Component({
  selector: 'app-not-found-records',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatPaginatorModule, // <-- Add MatPaginatorModule
    MatIconModule       // <-- Add MatIconModule
  ],
  templateUrl: './not-found-records.component.html',
  styleUrls: ['./not-found-records.component.scss']
})
export class NotFoundRecordsComponent implements OnInit, AfterViewInit {
  // Renamed 'psn' to 'pensionerId' for clarity with styling, adjust if your model uses 'psn'
  public displayedColumns: string[] = ['fullName', 'psn', 'department', 'ministry'];
  public dataSource = new MatTableDataSource<MasterListRecord>();
  public isLoading = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(private tenantService: TenantService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.tenantService.getFlaggedNotInSot().subscribe({
      next: (data) => {
        this.dataSource.data = data; // <-- Populate dataSource
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Failed to load 'not found' records", err);
        this.dataSource.data = []; // <-- Ensure data is empty on error
        this.isLoading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator; // <-- Connect paginator
  }
  
  /**
   * Generates initials from a full name for the avatar.
   * @param fullName The full name of the person.
   * @returns Two-letter initials.
   */
  getInitials(fullName: string): string {
    if (!fullName) {
      return '??';
    }
    const names = fullName.split(' ');
    const firstNameInitial = names[0] ? names[0][0] : '';
    const lastNameInitial = names.length > 1 ? names[names.length - 1][0] : '';
    return `${firstNameInitial}${lastNameInitial}`.toUpperCase();
  }
}