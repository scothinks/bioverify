// src/app/liveness-check/liveness-check.component.ts
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebcamModule, WebcamImage } from 'ngx-webcam';
import { LivenessService } from '../services/liveness.service';
import { Subject, Observable } from 'rxjs';

// Import Angular Material Modules
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-liveness-check',
  standalone: true,
  imports: [CommonModule, WebcamModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule],
  templateUrl: './liveness-check.component.html',
  styleUrls: ['./liveness-check.component.scss']
})
export class LivenessCheckComponent {
  @Output() checkCompleted = new EventEmitter<void>();
  
  public webcamImage: WebcamImage | null = null;
  private trigger: Subject<void> = new Subject<void>();

  isLoading = false;
  errorMessage = '';

  constructor(private livenessService: LivenessService) { }

  public triggerSnapshot(): void {
    this.trigger.next();
  }

  public handleImage(webcamImage: WebcamImage): void {
    this.webcamImage = webcamImage;
    // For a real liveness check, you might capture a short video or multiple frames.
    // For this example, we'll submit a single snapshot.
    this.submitCheck();
  }

  public get trigger$(): Observable<void> {
    return this.trigger.asObservable();
  }

  private submitCheck(): void {
    if (!this.webcamImage) return;

    this.isLoading = true;
    this.errorMessage = '';
    
    const imageFile = this.dataUrlToFile(this.webcamImage.imageAsDataUrl, 'liveness.jpg');

    this.livenessService.submitLivenessCheck(imageFile).subscribe({
      next: () => {
        this.isLoading = false;
        this.checkCompleted.emit(); // Notify the parent dashboard
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Liveness check failed. Please try again.';
        this.webcamImage = null; // Allow user to retry
      }
    });
  }

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