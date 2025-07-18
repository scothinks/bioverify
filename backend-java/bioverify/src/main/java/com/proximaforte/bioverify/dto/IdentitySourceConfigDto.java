package com.proximaforte.bioverify.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentitySourceConfigDto {
    private String providerName;
    private String apiBaseUrl;
    private String tokenUri;
    private String clientId;
    private String clientSecretEncrypted;
    private String enrollmentUrl;
}