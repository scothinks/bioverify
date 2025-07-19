package com.proximaforte.bioverify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SotProfileDto {
    // Top-level fields
    private String ssid;
    private String nin;
    private String bvn;
    private String biometricStatus;

    // Nested objects to match the API response
    private IdentityDto identity;
    private ContactDto contact;
    private CivilServiceProfileDto civilServiceProfile;

    // --- NESTED CLASSES ---

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentityDto {
        private String surname;
        private String firstName;
        private String middleName;
        private String dateOfBirth;
        private String sex;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactDto {
        private String residentialAddress;
        private String phoneNumber;
        private String email;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CivilServiceProfileDto {
        private String psn;
        private String gradeLevel;
        private String dateOfFirstAppointment;
        private String dateOfConfirmation;
        private String cadre;
        private String stateMinistry;
        private boolean onTransfer;
    }
}