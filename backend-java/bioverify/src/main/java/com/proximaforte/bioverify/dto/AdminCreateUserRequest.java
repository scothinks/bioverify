package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;

public class AdminCreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}