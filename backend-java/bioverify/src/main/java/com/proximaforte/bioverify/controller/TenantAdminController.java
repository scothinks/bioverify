package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for administrative actions related to Tenants.
 * This will be secured later to only allow Global Super Admins.
 */
@RestController
@RequestMapping("/api/admin/tenants") // All endpoints in this class will start with this path
public class TenantAdminController {

    private final TenantService tenantService;

    @Autowired
    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * API endpoint to create a new tenant.
     * Listens for HTTP POST requests to /api/admin/tenants.
     * The request body should be a JSON object representing the new tenant.
     *
     * @param tenant The tenant object deserialized from the request body.
     * @return A ResponseEntity containing the created tenant and an HTTP 201 (Created) status.
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant createdTenant = tenantService.createTenant(tenant);
        return new ResponseEntity<>(createdTenant, HttpStatus.CREATED);
    }

    /**
     * API endpoint to get a list of all tenants.
     * Listens for HTTP GET requests to /api/admin/tenants.
     *
     * @return A list of all tenants in the system.
     */
    @GetMapping
    public List<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }
}