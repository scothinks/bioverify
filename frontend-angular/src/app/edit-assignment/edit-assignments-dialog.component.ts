import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { Ministry } from '../models/ministry.model';
import { Department } from '../models/department.model';
import { ReviewerData } from '../services/tenant.service';

@Component({
  selector: 'app-edit-assignments-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Edit Assignments for {{ data.reviewer.fullName }}</h2>
    <mat-dialog-content [formGroup]="editForm">
      <mat-form-field appearance="outline">
        <mat-label>Assigned Ministries</mat-label>
        <mat-select formControlName="ministryIds" multiple>
          <mat-option *ngFor="let ministry of data.allMinistries" [value]="ministry.id">
            {{ ministry.name }}
          </mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Assigned Departments</mat-label>
        <mat-select formControlName="departmentIds" multiple>
          <mat-option *ngFor="let dept of data.allDepartments" [value]="dept.id">
            {{ dept.name }}
          </mat-option>
        </mat-select>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!editForm.dirty" (click)="onSave()">Save Changes</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding-top: 20px;
    }
  `]
})
export class EditAssignmentsDialogComponent {
  editForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<EditAssignmentsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { reviewer: ReviewerData, allMinistries: Ministry[], allDepartments: Department[] },
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({
      ministryIds: [data.reviewer.assignedMinistries.map(m => m.id)],
      departmentIds: [data.reviewer.assignedDepartments.map(d => d.id)]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.editForm.value);
  }
}