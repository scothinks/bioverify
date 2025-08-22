import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantService } from '../services/tenant.service';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { User } from '../models/user.model';
import { Ministry } from '../models/ministry.model';
import { Department } from '../models/department.model';
import { Observable, Subscription } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReviewerManagementComponent } from '../reviewer-management/reviewer-management.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ReviewerManagementComponent
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit, OnDestroy {

  displayedColumns: string[] = ['fullName', 'email', 'role', 'actions'];
  dataSource = new MatTableDataSource<User>();
  userForm: FormGroup;
  roles: string[] = ['TENANT_ADMIN', 'REVIEWER', 'AGENT'];
  isLoading = true;
  showReviewerFields = false;
  
  public hidePassword = true;
  
  ministries$: Observable<Ministry[]>;
  departments$: Observable<Department[]>;

  private roleChangesSub!: Subscription;

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    if (paginator) {
      this.dataSource.paginator = paginator;
    }
  }

  constructor(
    private tenantService: TenantService,
    private fb: FormBuilder
  ) {
    this.userForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      role: ['', Validators.required],
      assignedMinistryIds: [[]],
      assignedDepartmentIds: [[]]
    });

    this.ministries$ = this.tenantService.getMinistries();
    this.departments$ = this.tenantService.getDepartments();
  }

  ngOnInit(): void {
    this.loadUsers();
    
    this.roleChangesSub = this.userForm.get('role')!.valueChanges.pipe(
      startWith(this.userForm.get('role')!.value)
    ).subscribe(role => {
      this.showReviewerFields = role === 'REVIEWER';
    });
  }

  ngOnDestroy(): void {
    if (this.roleChangesSub) {
      this.roleChangesSub.unsubscribe();
    }
  }

  loadUsers(): void {
    this.isLoading = true;
    this.tenantService.getUsers().subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Failed to load users", err);
        this.isLoading = false;
      }
    });
  }


  generatePassword(): void {
    const length = 12;
    const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+~`|}{[]:?><,./-=";
    let password = "";
    for (let i = 0, n = charset.length; i < length; ++i) {
      password += charset.charAt(Math.floor(Math.random() * n));
    }
    this.userForm.get('password')?.setValue(password);
  }

  onSubmit(): void {
    if (!this.userForm.valid) return;

    this.tenantService.createUser(this.userForm.value).subscribe({
        next: () => {
            this.loadUsers();
            this.userForm.reset();
            Object.keys(this.userForm.controls).forEach(key => {
                this.userForm.get(key)?.setErrors(null);
                this.userForm.get(key)?.markAsPristine();
                this.userForm.get(key)?.markAsUntouched();
            });
        },
        error: (err) => {
            console.error("Failed to create user", err);
        }
    });
  }

  getInitials(fullName: string): string {
    if (!fullName) return '?';
    const names = fullName.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }

  getRoleClass(role: string): string {
    return `role-${role?.toLowerCase()}`;
  }
}