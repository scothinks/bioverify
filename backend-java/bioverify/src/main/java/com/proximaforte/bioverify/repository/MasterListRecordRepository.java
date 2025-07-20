package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.enums.RecordStatus; // <-- Add this import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {

    List<MasterListRecord> findAllByTenantId(UUID tenantId);

    Optional<MasterListRecord> findBySsidHashAndNinHash(String ssidHash, String ninHash);

    Optional<MasterListRecord> findByUserId(UUID userId);

    Optional<MasterListRecord> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MasterListRecord> findByPsnHashAndTenantId(String psnHash, UUID tenantId);

    List<MasterListRecord> findAllByUploadIdAndStatus(UUID uploadId, RecordStatus status);

    List<MasterListRecord> findAllByTenantIdAndStatus(UUID tenantId, RecordStatus status);
}