// FILE: src/main/java/com/proximaforte/bioverify/dto/RegisterRequest.java

package com.proximaforte.bioverify.dto;

import com.proximaforte.bioverify.domain.enums.Role;
import java.util.UUID; // <-- IMPORT

public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private UUID tenantId; // <-- ADD THIS FIELD

    // getters and setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UUID getTenantId() { return tenantId; } // <-- ADD GETTER/SETTER
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
