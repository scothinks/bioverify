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
    
    // NEW: Add query to fetch record with its department and ministry
    @Query("SELECT r FROM MasterListRecord r " +
           "LEFT JOIN FETCH r.department " +
           "LEFT JOIN FETCH r.ministry " +
           "WHERE r.id = :id")
    Optional<MasterListRecord> findByIdWithDetails(@Param("id") UUID id);

    Optional<MasterListRecord> findByPsnHashAndTenantId(String psnHash, UUID tenantId);

    List<MasterListRecord> findAllByTenantIdAndStatus(UUID tenantId, RecordStatus status);

    List<MasterListRecord> findByTenantIdAndStatusIn(UUID tenantId, List<RecordStatus> statuses);

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
    
    List<MasterListRecord> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, RecordStatus status);
    
    // --- METHODS FOR DASHBOARD STATS ---

    /**
     * Counts the total number of unique records for a tenant.
     */
    long countByTenantId(UUID tenantId);

    /**
     * Counts the number of records for a tenant that have a specific status.
     * This will be used for Validated, Mismatched, Not Found, etc.
     */
    long countByTenantIdAndStatus(UUID tenantId, RecordStatus status);

    /**
     * Counts the number of records for a tenant that have a status within the given list.
     * This is used for calculating the "Total Verified" metric.
     */
    long countByTenantIdAndStatusIn(UUID tenantId, List<RecordStatus> statuses);
}