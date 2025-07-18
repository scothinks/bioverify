package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role;
import lombok.Getter;
import java.util.UUID;

@Getter
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
}