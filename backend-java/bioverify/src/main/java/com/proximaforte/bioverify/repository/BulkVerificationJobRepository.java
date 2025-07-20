package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.BulkVerificationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkVerificationJobRepository extends JpaRepository<BulkVerificationJob, UUID> {
    List<BulkVerificationJob> findAllByTenantId(UUID tenantId);
}