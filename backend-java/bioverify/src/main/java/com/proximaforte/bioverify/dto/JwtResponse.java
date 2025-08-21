package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class JwtResponse {
    private String accessToken;
    private String type = "Bearer";
    private String refreshToken;
    private UUID id;
    private String email;
    private Role role;

    public JwtResponse(String accessToken, String refreshToken, UUID id, String email, Role role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.role = role;
    }
}