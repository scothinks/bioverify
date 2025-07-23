package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.Department;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Ministry;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {

    List<MasterListRecord> findAllByTenantId(UUID tenantId);

    Optional<MasterListRecord> findBySsidHashAndNinHash(String ssidHash, String ninHash);

    Optional<MasterListRecord> findByUserId(UUID userId);

    Optional<MasterListRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MasterListRecord> findByPsnHashAndTenantId(String psnHash, UUID tenantId);

    List<MasterListRecord> findAllByTenantIdAndStatus(UUID tenantId, RecordStatus status);

    List<MasterListRecord> findByTenantIdAndStatusIn(UUID tenantId, List<RecordStatus> statuses);

    /**
     * Finds all records for export, eagerly fetching the 'validatedBy' user.
     * This now fetches ALL validated records for a complete snapshot.
     */
    @Query("SELECT r FROM MasterListRecord r LEFT JOIN FETCH r.validatedBy WHERE r.tenant.id = :tenantId AND r.status = :status")
    List<MasterListRecord> findAllToExport(@Param("tenantId") UUID tenantId, @Param("status") RecordStatus status);


    @Query("SELECT r FROM MasterListRecord r WHERE r.tenant.id = :tenantId " +
           "AND r.status IN :statuses " +
           "AND (r.department IN :departments OR r.ministry IN :ministries)")
    List<MasterListRecord> findRecordsByReviewerAssignments(
            @Param("tenantId") UUID tenantId,
            @Param("statuses") List<RecordStatus> statuses,
            @Param("departments") Set<Department> departments,
            @Param("ministries") Set<Ministry> ministries
    );
    
    /**
     * Finds all records for a tenant with a specific status, ordered by most recently created.
     * Used to fetch the list of records not found in the Source of Truth.
     */
    List<MasterListRecord> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, RecordStatus status);
}