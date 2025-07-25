package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.CreateTenantRequest;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
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
    private final ObjectMapper objectMapper; // NEW: Inject ObjectMapper

    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        tenantRepository.findBySubdomain(request.getSubdomain()).ifPresent(t -> {
            throw new IllegalStateException("A tenant with subdomain '" + t.getSubdomain() + "' already exists.");
        });

        // Create the config object from the request
        IdentitySourceConfigDto config = new IdentitySourceConfigDto();
        config.setProviderName(request.getProviderName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setClientId(request.getClientId());
        config.setClientSecretEncrypted(request.getClientSecretEncrypted());
        config.setAesKey(request.getAesKey());
        config.setIv(request.getIv());

        // Create the new Tenant entity from the DTO
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSubdomain(request.getSubdomain());
        tenant.setStateCode(request.getStateCode());
        tenant.setDescription(request.getDescription());
        
        // Convert the config object to a JSON string and save it
        try {
            tenant.setIdentitySourceConfig(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize identity source config", e);
        }
        
        return tenantRepository.save(tenant);
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
    
    public Optional<Tenant> getTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    public boolean isSubdomainAvailable(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain).isEmpty();
    }

    @Transactional
    public Tenant updateTenant(UUID id, CreateTenantRequest request) { // Use DTO for updates as well
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Tenant not found with id: " + id));

        // Update basic fields
        tenant.setName(request.getName());
        tenant.setStateCode(request.getStateCode());
        tenant.setDescription(request.getDescription());
        
        // Update the config object
        IdentitySourceConfigDto config = new IdentitySourceConfigDto();
        config.setProviderName(request.getProviderName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setClientId(request.getClientId());
        config.setClientSecretEncrypted(request.getClientSecretEncrypted());
        config.setAesKey(request.getAesKey());
        config.setIv(request.getIv());

        try {
            tenant.setIdentitySourceConfig(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize identity source config for update", e);
        }

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