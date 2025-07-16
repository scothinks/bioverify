package com.proximaforte.bioverify.dto;

public class AgentOnboardRequest {
    private String ssid;
    private String nin;
    private String email;

    // Getters and Setters
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}