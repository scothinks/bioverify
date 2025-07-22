package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private UUID tenantId;
    private Set<UUID> assignedMinistryIds;
    private Set<UUID> assignedDepartmentIds;
}