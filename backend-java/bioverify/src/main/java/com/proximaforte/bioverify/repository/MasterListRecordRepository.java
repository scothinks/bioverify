package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.MasterListRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {

    /**
     * Finds all records that belong to a specific tenant.
     * This is used by the VerificationService to perform a secure, in-memory search
     * against encrypted data.
     * @param tenantId The ID of the tenant.
     * @return A list of all records for that tenant.
     */
    List<MasterListRecord> findAllByTenantId(UUID tenantId);
}
