package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.CreateTenantRequest;
import com.proximaforte.bioverify.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID; // NEW: Import UUID

@RestController
@RequestMapping("/api/v1/global-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GLOBAL_SUPER_ADMIN')")
public class GlobalAdminController {

    private final TenantService tenantService;

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody CreateTenantRequest request) {
        Tenant newTenant = tenantService.createTenant(request);
        return ResponseEntity.ok(newTenant);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }
    
    // NEW: Add the endpoint for updating a tenant
    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable UUID tenantId,
            @RequestBody CreateTenantRequest request) {
        Tenant updatedTenant = tenantService.updateTenant(tenantId, request);
        return ResponseEntity.ok(updatedTenant);
    }
}