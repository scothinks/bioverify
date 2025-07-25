package com.proximaforte.bioverify.dto;

import lombok.Data;

@Data
public class CreateTenantRequest {
    // Basic Tenant Info
    private String name;
    private String subdomain;
    private String stateCode;
    private String description;

    // Identity Source Configuration
    private String providerName;
    private String apiBaseUrl;
    private String clientId;
    private String clientSecretEncrypted;
    private String aesKey;
    private String iv;
}