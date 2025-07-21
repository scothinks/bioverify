package com.proximaforte.bioverify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SotProfileDto {
    // Identity fields
    private String surname;
    private String firstName;
    private String middleName;
    private String dateOfBirth;
    private String sex;

    // Contact fields
    private String residentialAddress;
    private String phoneNumber;
    private String email;

    // Civil Service Profile fields
    private String psn;
    private String gradeLevel;
    private String dateOfFirstAppointment;
    private String dateOfConfirmation;
    private String cadre;
    private String stateMinistry;
    private boolean onTransfer;
    
    // Top-level fields
    private String ssid;
    private String nin;
    private String bvn;
    private String biometricStatus;
}