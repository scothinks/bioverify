package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/global-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GLOBAL_SUPER_ADMIN')")
public class GlobalAdminController {

    private final TenantService tenantService;

    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant newTenant = tenantService.createTenant(tenant);
        return ResponseEntity.ok(newTenant);
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }
}