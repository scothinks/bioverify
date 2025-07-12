package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the Tenant entity.
 * This interface provides all the standard CRUD (Create, Read, Update, Delete)
 * operations for the 'tenants' table.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by their unique subdomain.
     * Spring Data JPA automatically creates the query for us based on the method name.
     *
     * @param subdomain The subdomain to search for (e.g., "kwara").
     * @return An Optional containing the found Tenant, or an empty Optional if not found.
     */
    Optional<Tenant> findBySubdomain(String subdomain);

}