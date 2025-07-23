package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.BulkVerificationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkVerificationJobRepository extends JpaRepository<BulkVerificationJob, UUID> {
    
    /**
     * Finds all jobs for a specific tenant.
     * @param tenantId The ID of the tenant.
     * @return A list of jobs for the tenant.
     */
    List<BulkVerificationJob> findAllByTenantId(UUID tenantId);

    /**
     * Finds all jobs for a specific tenant, ordered by the most recently initiated first.
     * This is used for the job history dashboard.
     * @param tenantId The ID of the tenant.
     * @return A sorted list of jobs.
     */
    List<BulkVerificationJob> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}