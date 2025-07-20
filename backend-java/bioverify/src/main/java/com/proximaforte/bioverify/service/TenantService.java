package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        tenantRepository.findBySubdomain(tenant.getSubdomain()).ifPresent(t -> {
            throw new IllegalStateException("A tenant with subdomain '" + t.getSubdomain() + "' already exists.");
        });
        return tenantRepository.save(tenant);
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    // --- NEW METHODS ---

    public Optional<Tenant> getTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    public boolean isSubdomainAvailable(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain).isEmpty();
    }

    @Transactional
    public Tenant updateTenant(UUID id, Tenant tenantDetails) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Tenant not found with id: " + id));

        tenant.setName(tenantDetails.getName());
        tenant.setStateCode(tenantDetails.getStateCode());
        // Subdomain is not updatable, so we don't set it here.
        // Add other updatable fields as needed, e.g., description
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        if (!tenantRepository.existsById(id)) {
            throw new IllegalStateException("Tenant not found with id: " + id);
        }
        tenantRepository.deleteById(id);
    }
}