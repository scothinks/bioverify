import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

@Component({
  selector: 'app-master-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './master-list.component.html',
  styleUrls: ['./master-list.component.scss']
})
export class MasterListComponent implements OnInit, OnDestroy {
  
  allRecords: MasterListRecord[] = [];
  filteredRecords: MasterListRecord[] = [];
  private filterSubscription!: Subscription;

  constructor(private tenantService: TenantService) {}

  ngOnInit(): void {
    this.loadRecords();

    // Listen for filter requests from the service
    this.filterSubscription = this.tenantService.recordsFilter$.subscribe(recordIds => {
      if (recordIds && recordIds.length > 0) {
        // If a list of IDs is provided, filter the records
        this.filteredRecords = this.allRecords.filter(record => recordIds.includes(record.id));
      } else {
        // If the filter is cleared (null), show all records
        this.filteredRecords = this.allRecords;
      }
    });
  }

  ngOnDestroy(): void {
    // Unsubscribe to prevent memory leaks
    if (this.filterSubscription) {
      this.filterSubscription.unsubscribe();
    }
  }

  loadRecords(): void {
    this.tenantService.getRecordsForTenant().subscribe(data => {
      this.allRecords = data;
      this.filteredRecords = data; // Initially, show all records
    });
  }
}