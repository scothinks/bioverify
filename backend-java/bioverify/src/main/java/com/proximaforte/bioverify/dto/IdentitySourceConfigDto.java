package com.proximaforte.bioverify.dto;

/**
 * A generic Data Transfer Object to hold the configuration for a tenant's
 * external identity source of truth (e.g., Optima, a state-run registry, etc.).
 * This class is designed to be flexible for any provider.
 */
public class IdentitySourceConfigDto {

    private String validationUrl;
    private String apiKey; // Placeholder for future use if providers require API keys

    // Getters and Setters

    public String getValidationUrl() {
        return validationUrl;
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
