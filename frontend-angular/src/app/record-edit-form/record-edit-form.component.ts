import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
// Uncomment if using the date picker
// import { MatDatepickerModule } from '@angular/material/datepicker';
// import { MatNativeDateModule } from '@angular/material/core';
import { MasterListRecord } from '../models/master-list-record.model';
import { TenantService } from '../services/tenant.service';

@Component({
  selector: 'app-record-edit-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    // MatDatepickerModule,
    // MatNativeDateModule
  ],
  templateUrl: './record-edit-form.component.html',
  styleUrls: ['./record-edit-form.component.scss']
})
export class RecordEditFormComponent implements OnInit {
  
  editForm: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<RecordEditFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MasterListRecord,
    private fb: FormBuilder,
    private tenantService: TenantService
  ) {
    // Initialize the form with data from the dialog
    this.editForm = this.fb.group({
      fullName: [this.data.fullName, Validators.required],
      department: [this.data.department, Validators.required],
      ministry: [this.data.ministry],
      gradeLevel: [this.data.gradeLevel],
      salaryStructure: [this.data.salaryStructure],
      dateOfBirth: [this.data.dateOfBirth]
    });
  }

  ngOnInit(): void {}

  onCancel(): void {
    // Close the dialog without sending any data back
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.editForm.valid) {
      this.tenantService.updateRecord(this.data.id, this.editForm.value).subscribe({
        next: (updatedRecord) => {
          // Close the dialog and send the updated record back to the queue component
          this.dialogRef.close(updatedRecord);
        },
        error: (err) => {
          console.error('Failed to update record:', err);
          alert('Error: ' + err.message);
        }
      });
    }
  }
}