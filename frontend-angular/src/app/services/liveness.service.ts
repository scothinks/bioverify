// src/app/services/liveness.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LivenessService {
  private apiUrl = '/api/v1/liveness';

  constructor(private http: HttpClient) { }

  submitLivenessCheck(livenessVideo: File): Observable<any> {
    const formData = new FormData();
    formData.append('livenessVideo', livenessVideo, livenessVideo.name);
    return this.http.post(`${this.apiUrl}/submit`, formData);
  }
}