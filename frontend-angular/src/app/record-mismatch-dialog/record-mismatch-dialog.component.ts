import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MasterListRecord } from '../models/master-list-record.model';

interface ComparisonRow {
  field: string;
  originalValue: any;
  sotValue: any;
  isMismatched: boolean;
}

@Component({
  selector: 'app-record-mismatch-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule
  ],
  templateUrl: './record-mismatch-dialog.component.html',
  styleUrls: ['./record-mismatch-dialog.component.scss']
})
export class RecordMismatchDialogComponent {
  
  public comparisonData: ComparisonRow[] = [];
  public displayedColumns: string[] = ['field', 'originalValue', 'sotValue'];

  constructor(
    public dialogRef: MatDialogRef<RecordMismatchDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public record: MasterListRecord
  ) {
    this.buildComparison();
  }

  private buildComparison(): void {
    const original = JSON.parse(this.record.originalUploadData || '{}');
    const sot = JSON.parse(this.record.sotData || '{}');
    
    const allKeys = new Set([...Object.keys(original), ...Object.keys(sot)]);
    
    allKeys.forEach(key => {
      const originalValue = original[key];
      const sotValue = sot[key];
      this.comparisonData.push({
        field: key,
        originalValue: originalValue,
        sotValue: sotValue,
        isMismatched: JSON.stringify(originalValue) !== JSON.stringify(sotValue)
      });
    });
  }

  onAcceptSot(): void {
    // Pass back a signal to the calling component to accept the SoT data
    this.dialogRef.close('accept');
  }

  onManualEdit(): void {
    // Pass back a signal to edit manually
    this.dialogRef.close('edit');
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}