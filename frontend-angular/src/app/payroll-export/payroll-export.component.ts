import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TenantService, PayrollExportLog } from '../services/tenant.service';

@Component({
  selector: 'app-payroll-export',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatTableModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './payroll-export.component.html',
  styleUrls: ['./payroll-export.component.scss']
})
export class PayrollExportComponent implements OnInit {
  public history: PayrollExportLog[] = [];
  public displayedColumns: string[] = ['exportTimestamp', 'recordCount', 'status', 'exportedByEmail', 'actions'];
  public isLoading = false;
  public isGenerating = false;

  constructor(
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.tenantService.getExportHistory().subscribe({
      next: (data) => {
        this.history = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.showSnackBar('Failed to load export history.');
        console.error(err);
      }
    });
  }

  generateNewExport(): void {
    this.isGenerating = true;
    this.tenantService.initiateExport().subscribe({
      next: () => {
        this.isGenerating = false;
        this.showSnackBar('Payroll export job initiated! The list will update shortly.', 'success-snackbar');
        // A simple refresh after 5 seconds to get the new job status
        setTimeout(() => this.loadHistory(), 5000); 
      },
      error: (err) => {
        this.isGenerating = false;
        this.showSnackBar('Failed to initiate export.');
        console.error(err);
      }
    });
  }

  downloadFile(log: PayrollExportLog): void {
    this.tenantService.downloadExportFile(log.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payroll-export-${log.id}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (err) => {
        this.showSnackBar('Failed to download file.');
        console.error(err);
      }
    });
  }

  private showSnackBar(message: string, panelClass: string = 'error-snackbar'): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: [panelClass]
    });
  }
}