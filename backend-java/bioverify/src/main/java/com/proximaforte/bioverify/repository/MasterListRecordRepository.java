package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.MasterListRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {

    List<MasterListRecord> findAllByTenantId(UUID tenantId);

    Optional<MasterListRecord> findBySsidHashAndNinHash(String ssidHash, String ninHash);
}