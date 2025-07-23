import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

@Component({
  selector: 'app-not-found-records',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatProgressSpinnerModule],
  templateUrl: './not-found-records.component.html',
  styleUrls: ['./not-found-records.component.scss']
})
export class NotFoundRecordsComponent implements OnInit {
  public records: MasterListRecord[] = [];
  public displayedColumns: string[] = ['fullName', 'psn', 'department', 'ministry'];
  public isLoading = false;

  constructor(private tenantService: TenantService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.tenantService.getFlaggedNotInSot().subscribe({
      next: (data) => {
        this.records = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Failed to load 'not found' records", err);
        this.isLoading = false;
      }
    });
  }
}