package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.PayrollExportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollExportLogRepository extends JpaRepository<PayrollExportLog, UUID> {

    /**
     * Finds all export logs for a specific tenant, ordered by the most recent first.
     *
     * @param tenantId The ID of the tenant.
     * @return A list of payroll export logs.
     */
    List<PayrollExportLog> findByTenantIdOrderByExportTimestampDesc(UUID tenantId);
}