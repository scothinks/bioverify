import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TenantService } from '../services/tenant.service';
import { TenantValidators } from '../validators/tenant.validators';
import { Tenant } from '../models/tenant.model';

// Import Angular Material modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './tenant-form.component.html',
  styleUrls: ['./tenant-form.component.scss']
})
export class TenantFormComponent implements OnInit {
  @Input() tenant: Tenant | null = null; // For edit mode
  @Output() tenantSaved = new EventEmitter<Tenant>();
  @Output() cancelled = new EventEmitter<void>();

  tenantForm: FormGroup;
  isLoading = false;
  isEditMode = false;

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {
    this.tenantForm = this.createForm();
  }

  ngOnInit(): void {
    if (this.tenant) {
      this.isEditMode = true;
      this.populateForm();
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      subdomain: [
        '', 
        [
          Validators.required, 
          Validators.minLength(2), 
          Validators.maxLength(50),
          TenantValidators.subdomainPattern()
        ],
        [TenantValidators.uniqueSubdomain(this.tenantService)]
      ],
      stateCode: ['', [Validators.required, TenantValidators.stateCodePattern()]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  private populateForm(): void {
    if (this.tenant) {
      this.tenantForm.patchValue({
        name: this.tenant.name,
        subdomain: this.tenant.subdomain,
        stateCode: this.tenant.stateCode,
        description: this.tenant.description || ''
      });
      
      // Disable subdomain field in edit mode to prevent conflicts
      this.tenantForm.get('subdomain')?.disable();
    }
  }

  getErrorMessage(fieldName: string): string {
    const control = this.tenantForm.get(fieldName);
    if (!control?.errors) return '';

    const errors = control.errors;
    
    if (errors['required']) return `${this.getFieldLabel(fieldName)} is required`;
    if (errors['minlength']) return `${this.getFieldLabel(fieldName)} must be at least ${errors['minlength'].requiredLength} characters`;
    if (errors['maxlength']) return `${this.getFieldLabel(fieldName)} must not exceed ${errors['maxlength'].requiredLength} characters`;
    if (errors['subdomainPattern']) return errors['subdomainPattern'].message;
    if (errors['stateCodePattern']) return errors['stateCodePattern'].message;
    if (errors['subdomainTaken']) return errors['subdomainTaken'].message;
    
    return 'Invalid input';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      name: 'Tenant Name',
      subdomain: 'Subdomain',
      stateCode: 'State Code',
      description: 'Description'
    };
    return labels[fieldName] || fieldName;
  }

  onSubmit(): void {
    if (this.tenantForm.invalid) {
      this.markAllFieldsAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.tenantForm.value;

    const operation = this.isEditMode 
      ? this.tenantService.updateTenant(this.tenant!.id, formValue)
      : this.tenantService.createTenant(formValue);

    operation.subscribe({
      next: (tenant: Tenant) => {
        this.isLoading = false;
        const action = this.isEditMode ? 'updated' : 'created';
        this.snackBar.open(`Tenant "${tenant.name}" ${action} successfully!`, 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        
        this.tenantSaved.emit(tenant);
        
        if (!this.isEditMode) {
          this.tenantForm.reset();
        }
      },
      error: (error: Error) => {
        this.isLoading = false;
        const action = this.isEditMode ? 'updating' : 'creating';
        this.snackBar.open(`Error ${action} tenant: ${error.message}`, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onCancel(): void {
    if (this.isEditMode) {
      this.cancelled.emit();
    } else {
      this.tenantForm.reset();
    }
  }

  private markAllFieldsAsTouched(): void {
    Object.keys(this.tenantForm.controls).forEach(key => {
      this.tenantForm.get(key)?.markAsTouched();
    });
  }
}