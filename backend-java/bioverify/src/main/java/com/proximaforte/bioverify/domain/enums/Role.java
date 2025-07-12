package com.proximaforte.bioverify.domain.enums;

public enum Role {
    GLOBAL_SUPER_ADMIN, // Can manage tenants
    TENANT_ADMIN,       // Can manage users and data for their own tenant
    FOCAL_OFFICER,      // Can validate records within their tenant
    ENUMERATOR,         // Can perform verifications (future use)
    SELF_SERVICE_USER   // A standard employee or pensioner
}
