package com.proximaforte.bioverify.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountRequest {
    private UUID recordId;
    private String email;
    private String password;
}