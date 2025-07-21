package com.proximaforte.bioverify.domain.enums;

public enum Role {
    GLOBAL_SUPER_ADMIN, // Can manage tenants
    TENANT_ADMIN,       // Can manage users and data for their own tenant
    REVIEWER,      // Can validate records within their tenant
    AGENT,         // Can perform verifications
    SELF_SERVICE_USER   // A standard employee or pensioner
}
