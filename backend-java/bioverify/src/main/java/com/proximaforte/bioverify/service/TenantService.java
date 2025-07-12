package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for handling business logic related to Tenants.
 * This acts as an intermediary between the Controller (API layer) and the Repository (database layer).
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Constructor-based dependency injection.
     * Spring will automatically provide an instance of TenantRepository.
     * @param tenantRepository The repository for tenant data access.
     */
    @Autowired
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Creates and saves a new tenant.
     * It first checks if a tenant with the same subdomain already exists to prevent duplicates.
     * @param tenant The Tenant object to be created.
     * @return The saved Tenant object, including its generated ID and timestamps.
     * @throws IllegalStateException if a tenant with the same subdomain already exists.
     */
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        Optional<Tenant> existingTenant = tenantRepository.findBySubdomain(tenant.getSubdomain());
        if (existingTenant.isPresent()) {
            throw new IllegalStateException("A tenant with subdomain '" + tenant.getSubdomain() + "' already exists.");
        }
        return tenantRepository.save(tenant);
    }

    /**
     * Retrieves all tenants from the database.
     * @return A list of all Tenant objects.
     */
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
}
