/**
 * Agent Layout Component - Proof of Life (PoL) Workflow Interface
 * 
 * This component provides the complete user interface for agents to conduct
 * Proof of Life verification sessions. The workflow includes:
 * 
 * 1. Employee Record Search: Find eligible records using SSID/NIN
 * 2. Live Photo Capture: Use webcam to capture employee photo
 * 3. Document Upload: Upload Letter of Employment and Work ID
 * 4. PoL Submission: Complete the verification process
 * 
 * Features:
 * - Real-time webcam integration for biometric capture
 * - Form validation for required fields and documents
 * - File upload handling for PDF documents
 * - Error handling and user feedback
 * - Automatic workflow state management
 * 
 * This is the primary interface for AGENT role users in the system.
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PolService, MasterListRecordDto, FindRecordRequest } from '../services/pol.service';
import { Subject, Observable } from 'rxjs';
import { WebcamImage, WebcamModule } from 'ngx-webcam';

// Angular Material UI components for professional interface
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
  // === FORM MANAGEMENT ===
  /** Form for searching employee records by SSID/NIN */
  searchForm!: FormGroup;
  /** Form for collecting email and initiating PoL process */
  polForm!: FormGroup;
  /** Currently selected employee record for PoL processing */
  selectedRecord: MasterListRecordDto | null = null;

  // === WEBCAM INTEGRATION ===
  /** Controls webcam visibility in the UI */
  public showWebcam = false;
  /** Stores the captured webcam image */
  public webcamImage: WebcamImage | null = null;
  /** RxJS Subject to trigger photo capture */
  private trigger: Subject<void> = new Subject<void>();

  // === FILE MANAGEMENT ===
  /** Captured employee photo file from webcam */
  capturedPhoto: File | null = null;
  /** Uploaded Letter of Employment PDF document */
  letterOfEmploymentFile: File | null = null;
  /** Uploaded Work ID PDF document */
  workIdFile: File | null = null;

  // === UI STATE MANAGEMENT ===
  /** Loading state for async operations */
  isLoading = false;
  /** Error message display */
  errorMessage = '';
  /** Success message display */
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private polService: PolService
  ) {}

  /**
   * Initializes reactive forms with validation rules.
   * Sets up the two-stage workflow: search form and PoL form.
   */
  ngOnInit(): void {
    // Search form requires both SSID and NIN for record identification
    this.searchForm = this.fb.group({
      ssid: ['', Validators.required],
      nin: ['', Validators.required]
    });

    // PoL form requires email for account creation and notifications
    this.polForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  // === MAIN WORKFLOW METHODS ===

  /**
   * Handles employee record search based on SSID/NIN.
   * Finds records that are eligible for Proof of Life processing.
   */
  onSearchSubmit(): void {
    if (this.searchForm.invalid) return;
    
    this.isLoading = true;
    this.resetMessages();
    this.selectedRecord = null;

    // Call backend to find eligible record
    this.polService.findRecordForPol(this.searchForm.value).subscribe({
      next: (record) => {
        this.isLoading = false;
        this.selectedRecord = record;
        // Pre-populate email if available from the record
        this.polForm.get('email')?.setValue(record.email || '');
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not find a record ready for Proof of Life.';
      }
    });
  }

  /**
   * Submits the complete Proof of Life package to the backend.
   * 
   * Required components:
   * - Selected employee record
   * - Captured employee photo
   * - Letter of Employment document
   * - Work ID document
   * - Verified email address
   */
  onPoLSubmit(): void {
    // Validate all required components are present
    if (this.polForm.invalid || !this.selectedRecord || !this.capturedPhoto || !this.letterOfEmploymentFile || !this.workIdFile) {
      this.errorMessage = 'All fields, a captured photo, and both required documents must be provided.';
      return;
    }

    this.isLoading = true;
    this.resetMessages();
    const verifiedEmail = this.polForm.get('email')?.value;

    // Submit complete PoL package to backend for processing
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
        this.resetToSearch(); // Return to initial state for next session
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Proof of Life submission failed.';
      }
    });
  }

  // === WEBCAM CONTROL METHODS ===

  /**
   * Triggers the webcam to capture a snapshot.
   * Uses RxJS Subject pattern to communicate with ngx-webcam component.
   */
  public triggerSnapshot(): void {
    this.trigger.next();
  }

  /**
   * Toggles the webcam display on/off.
   * Used to show/hide the camera interface in the UI.
   */
  public toggleWebcam(): void {
    this.showWebcam = !this.showWebcam;
  }

  /**
   * Handles the captured webcam image and converts it to a File object.
   * Automatically hides the webcam after successful capture.
   * 
   * @param webcamImage The image captured from the webcam component
   */
  public handleImage(webcamImage: WebcamImage): void {
    this.webcamImage = webcamImage;
    // Convert base64 data URL to File object for form submission
    this.capturedPhoto = this.dataUrlToFile(webcamImage.imageAsDataUrl, 'capture.jpg');
    this.showWebcam = false; // Hide webcam interface after capture
  }

  /**
   * Observable trigger for webcam snapshot functionality.
   * Required by ngx-webcam component for external trigger control.
   */
  public get trigger$(): Observable<void> {
    return this.trigger.asObservable();
  }

  // === DOCUMENT UPLOAD METHODS ===

  /**
   * Handles Letter of Employment file selection.
   * Validates and stores the uploaded PDF document.
   */
  onLetterOfEmploymentSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element.files?.[0];
    if (file) this.letterOfEmploymentFile = file;
  }

  /**
   * Handles Work ID file selection.
   * Validates and stores the uploaded PDF document.
   */
  onWorkIdSelected(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element.files?.[0];
    if (file) this.workIdFile = file;
  }

  // === WORKFLOW RESET METHODS ===

  /**
   * Resets the component to initial search state.
   * Clears all forms, files, and state for the next PoL session.
   */
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

  /**
   * Clears all user feedback messages.
   * Used to provide clean UI state between operations.
   */
  private resetMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * Utility function to convert webcam base64 data URL to File object.
   * Required for multipart form submission to the backend API.
   * 
   * @param dataUrl Base64 encoded image data URL from webcam
   * @param filename Desired filename for the created File object
   * @returns File object ready for form submission
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