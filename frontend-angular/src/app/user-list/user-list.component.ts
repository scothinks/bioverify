import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { Observable } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { User } from '../models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {

  public users$!: Observable<User[]>;
  displayedColumns: string[] = ['fullName', 'email', 'role'];
  userForm: FormGroup;
  // CORRECTED the role name from ENUMERATOR to AGENT
  roles: string[] = ['TENANT_ADMIN', 'REVIEWER', 'AGENT']; 

  constructor(
    private tenantService: TenantService,
    private fb: FormBuilder
  ) {
    this.userForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.users$ = this.tenantService.getUsers();
  }

  onSubmit(): void {
    if (this.userForm.valid) {
      this.tenantService.createUser(this.userForm.value).subscribe(() => {
        this.loadUsers(); // Refresh user list on success
        this.userForm.reset(); // Clear the form
        // Untouch the fields to remove error states
        Object.keys(this.userForm.controls).forEach(key => {
          this.userForm.get(key)?.setErrors(null) ;
          this.userForm.get(key)?.markAsPristine();
          this.userForm.get(key)?.markAsUntouched();
        });
      });
    }
  }
}