// src/app/services/pol.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// This interface is aligned with the complete backend MasterListRecordDto
export interface MasterListRecordDto {
  id: string;
  employeeId: string; // WID
  fullName: string;
  psn: string;
  ssid: string;
  nin: string;
  department: string;
  ministry: string;
  gradeLevel: string;
  salaryStructure: string;
  status: string;
  biometricStatus: boolean;
  validatedAt: string;
  validatedByEmail: string;
  createdAt: string;
  bvn: string;
  dateOfBirth: string;
  gender: string;
  phoneNumber: string;
  email: string;
  lastLivenessCheckDate: string | null;
  nextLivenessCheckDate: string | null;
}

export interface FindRecordRequest {
  ssid: string;
  nin: string;
}

@Injectable({
  providedIn: 'root'
})
export class PolService {
  private apiUrl = '/api/v1';

  constructor(private http: HttpClient) { }

  /**
   * Finds a record by identifiers that is ready for PoL.
   */
  findRecordForPol(request: FindRecordRequest): Observable<MasterListRecordDto> {
    return this.http.post<MasterListRecordDto>(`${this.apiUrl}/records/find-for-pol`, request);
  }

  /**
   * Submits the PoL data with specifically named document files.
   */
  completePol(
    recordId: string,
    email: string,
    photo: File,
    letterOfEmployment: File,
    workId: File
  ): Observable<any> {
    const formData = new FormData();
    formData.append('email', email);
    formData.append('photo', photo, photo.name);
    formData.append('letterOfEmployment', letterOfEmployment, letterOfEmployment.name);
    formData.append('workId', workId, workId.name);

    return this.http.post(`${this.apiUrl}/pol/${recordId}/complete`, formData);
  }
}