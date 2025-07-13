package com.proximaforte.bioverify.dto;

/**
 * Data Transfer Object for a verification request.
 * Contains the credentials a user provides to verify their identity.
 */
public class VerificationRequest {
    private String ssid;
    private String nin;

    // Getters and Setters
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }
}