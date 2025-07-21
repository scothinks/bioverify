package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
}