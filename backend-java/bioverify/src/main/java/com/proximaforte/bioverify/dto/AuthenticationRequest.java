package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/**
 * A consolidated DTO for all authentication and user creation requests.
 * Different fields are used depending on the specific action.
 */
@Getter
@Setter
public class AuthenticationRequest {
    
    // For login and employee account creation
    private String email;
    private String password;
    
    // For creating admin users
    private String fullName;
    private Role role;
    private UUID tenantId;

    // For creating an account after verification
    private UUID recordId;
}