// src/app/agent-dashboard/agent-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PolService, MasterListRecordDto, FindRecordRequest } from '../services/pol.service';
import { Subject, Observable } from 'rxjs';
import { WebcamImage, WebcamModule } from 'ngx-webcam';

// Import Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-agent-layout',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatDividerModule,
    WebcamModule
  ],
  templateUrl: './agent-layout.component.html',
  styleUrls: ['./agent-layout.component.scss']
})
export class AgentLayoutComponent implements OnInit {
  searchForm!: FormGroup;
  polForm!: FormGroup;
  selectedRecord: MasterListRecordDto | null = null;

  // --- Webcam Properties ---
  public showWebcam = false;
  public webcamImage: WebcamImage | null = null;
  private trigger: Subject<void> = new Subject<void>();

  // --- Form File Properties ---
  capturedPhoto: File | null = null;
  letterOfEmploymentFile: File | null = null;
  workIdFile: File | null = null;

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private polService: PolService
  ) {}

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required]
    });

    this.polForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  // --- Main Workflow Methods ---

  onSearchSubmit(): void {
    if (this.searchForm.invalid) return;
    this.isLoading = true;
    this.resetMessages();
    this.selectedRecord = null;

    this.polService.findRecordForPol(this.searchForm.value).subscribe({
      next: (record) => {
        this.isLoading = false;
        this.selectedRecord = record;
        this.polForm.get('email')?.setValue(record.email || '');
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not find a record ready for Proof of Life.';
      }
    });
  }

  onPoLSubmit(): void {
    if (this.polForm.invalid || !this.selectedRecord || !this.capturedPhoto || !this.letterOfEmploymentFile || !this.workIdFile) {
      this.errorMessage = 'All fields, a captured photo, and both required documents must be provided.';
      return;
    }
    this.isLoading = true;
    this.resetMessages();
    const verifiedEmail = this.polForm.get('email')?.value;

    this.polService.completePol(
      this.selectedRecord.id,
      verifiedEmail,
      this.capturedPhoto,
      this.letterOfEmploymentFile,
      this.workIdFile
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = `Successfully processed PoL for ${this.selectedRecord?.fullName}. The submission is now pending automated validation.`;
        this.resetToSearch();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Proof of Life submission failed.';
      }
    });
  }

  // --- Webcam Control Methods ---

  public triggerSnapshot(): void {
    this.trigger.next();
  }

  public toggleWebcam(): void {
    this.showWebcam = !this.showWebcam;
  }

  public handleImage(webcamImage: WebcamImage): void {
    this.webcamImage = webcamImage;
    this.capturedPhoto = this.dataUrlToFile(webcamImage.imageAsDataUrl, 'capture.jpg');
    this.showWebcam = false; // Hide the webcam view after capture
  }

  public get trigger$(): Observable<void> {
    return this.trigger.asObservable();
  }

  // --- Document and Reset Methods ---

  onLetterOfEmploymentSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element.files?.[0];
    if (file) this.letterOfEmploymentFile = file;
  }

  onWorkIdSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element.files?.[0];
    if (file) this.workIdFile = file;
  }

  resetToSearch(): void {
    this.selectedRecord = null;
    this.capturedPhoto = null;
    this.webcamImage = null;
    this.letterOfEmploymentFile = null;
    this.workIdFile = null;
    this.searchForm.reset();
    this.polForm.reset();
    this.resetMessages();
  }

  private resetMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * Helper function to convert a base64 data URL to a File object.
   */
  private dataUrlToFile(dataUrl: string, filename: string): File {
    const arr = dataUrl.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1];
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  }
}