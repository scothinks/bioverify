import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AsyncValidatorFn } from '@angular/forms';
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

  tenantForm!: FormGroup;
  isLoading = false;
  isEditMode = false;

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.tenant;
    this.createForm();
    if (this.isEditMode) {
      this.populateForm();
    }
  }

  private createForm(): void {
    // Conditionally set the async validator only for create mode
    const subdomainAsyncValidators: AsyncValidatorFn[] = !this.isEditMode
      ? [TenantValidators.uniqueSubdomain(this.tenantService)]
      : [];

    this.tenantForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      subdomain: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(50),
          TenantValidators.subdomainPattern()
        ],
        subdomainAsyncValidators // Use the conditionally set validators
      ],
      stateCode: ['', [Validators.required, TenantValidators.stateCodePattern()]],
      description: ['', [Validators.maxLength(500)]],
      validationUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]]
    });
  }

  private populateForm(): void {
    if (this.tenant) {
      let validationUrl = '';
      if (this.tenant.identitySourceConfig) {
          try {
              const config = JSON.parse(this.tenant.identitySourceConfig);
              validationUrl = config.validationUrl || '';
          } catch (e) {
              console.error("Could not parse tenant config", e);
          }
      }

      this.tenantForm.patchValue({
        name: this.tenant.name,
        subdomain: this.tenant.subdomain,
        stateCode: this.tenant.stateCode,
        description: this.tenant.description || '',
        validationUrl: validationUrl
      });
      this.tenantForm.get('subdomain')?.disable();
    }
  }

  getErrorMessage(fieldName: string): string {
    const control = this.tenantForm.get(fieldName);
    if (!control?.errors || !control.touched) return '';
    const errors = control.errors;
    
    if (errors['required']) return `This field is required`;
    if (errors['minlength']) return `Must be at least ${errors['minlength'].requiredLength} characters`;
    if (errors['maxlength']) return `Cannot exceed ${errors['maxlength'].requiredLength} characters`;
    if (errors['pattern']) return `Please enter a valid URL`;
    if (errors['subdomainPattern']) return errors['subdomainPattern'].message;
    if (errors['stateCodePattern']) return errors['stateCodePattern'].message;
    if (errors['subdomainTaken']) return errors['subdomainTaken'].message;
    
    return 'Invalid input';
  }

  onSubmit(): void {
    if (this.tenantForm.invalid) {
      this.tenantForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const formValue = this.tenantForm.getRawValue();
    const payload = {
        name: formValue.name,
        subdomain: formValue.subdomain,
        stateCode: formValue.stateCode,
        description: formValue.description,
        identitySourceConfig: JSON.stringify({ validationUrl: formValue.validationUrl })
    };

    const operation = this.isEditMode
      ? this.tenantService.updateTenant(this.tenant!.id, payload)
      : this.tenantService.createTenant(payload);

    operation.subscribe({
      next: (tenant: Tenant) => {
        this.isLoading = false;
        const action = this.isEditMode ? 'updated' : 'created';
        this.snackBar.open(`Tenant "${tenant.name}" ${action} successfully!`, 'Close', {
          duration: 3000, panelClass: ['success-snackbar']
        });
        this.tenantSaved.emit(tenant);
      },
      error: (error: Error) => {
        this.isLoading = false;
        const action = this.isEditMode ? 'updating' : 'creating';
        this.snackBar.open(`Error ${action} tenant: ${error.message}`, 'Close', {
          duration: 5000, panelClass: ['error-snackbar']
        });
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}