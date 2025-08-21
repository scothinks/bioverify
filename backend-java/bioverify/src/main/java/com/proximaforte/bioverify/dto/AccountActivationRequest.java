package com.proximaforte.bioverify.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountActivationRequest {
    private String token;
    private String password;
}