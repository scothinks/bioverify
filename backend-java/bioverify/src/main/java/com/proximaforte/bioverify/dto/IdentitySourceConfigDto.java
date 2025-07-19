package com.proximaforte.bioverify.dto;

import com.fasterxml.jackson.annotation.JsonProperty; 
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentitySourceConfigDto {
    private String providerName;
    private String apiBaseUrl;
    private String tokenUri;

    @JsonProperty("client-id") 
    private String clientId;

    private String clientSecretEncrypted;
    private String enrollmentUrl;
    private String aesKey;
    private String iv;
}