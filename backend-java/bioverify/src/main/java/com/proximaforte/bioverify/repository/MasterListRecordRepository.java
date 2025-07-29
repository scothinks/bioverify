package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.Department;
import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.Ministry;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // NEW: Import LocalDate
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {

    List<MasterListRecord> findAllByTenantId(UUID tenantId);

    Optional<MasterListRecord> findByTenantIdAndSsidHashAndNinHash(UUID tenantId, String ssidHash, String ninHash);

    Optional<MasterListRecord> findByUserId(UUID userId);

    Optional<MasterListRecord> findByIdAndTenantId(UUID id, UUID tenantId);

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
    
    // --- NEW METHODS FOR LIVENESS CHECK SCHEDULER ---

    /**
     * Finds records where the liveness check is due on a specific date (for notifications).
     */
    List<MasterListRecord> findAllByStatusAndNextLivenessCheckDate(RecordStatus status, LocalDate nextLivenessCheckDate);

    /**
     * Finds records where the liveness check due date is in the past (for suspension).
     */
    List<MasterListRecord> findAllByStatusAndNextLivenessCheckDateBefore(RecordStatus status, LocalDate nextLivenessCheckDate);
    
    // --- METHODS FOR DASHBOARD STATS ---

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, RecordStatus status);

    long countByTenantIdAndStatusIn(UUID tenantId, List<RecordStatus> statuses);
}