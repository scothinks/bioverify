package com.proximaforte.bioverify.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyIdentityRequest {
    private String ssid;
    private String nin;
}