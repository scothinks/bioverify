package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role;
import java.util.UUID;

public class UserDto {

    private UUID id;
    private String fullName;
    private String email;
    private Role role;

    public UserDto(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    // Getters
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
}