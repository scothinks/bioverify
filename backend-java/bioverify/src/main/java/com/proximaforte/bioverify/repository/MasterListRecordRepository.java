package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.MasterListRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the MasterListRecord entity.
 * This interface provides all the standard CRUD operations for the
 * 'master_list_records' table. We can add custom query methods here later.
 */
@Repository
public interface MasterListRecordRepository extends JpaRepository<MasterListRecord, UUID> {
    // Spring Data JPA will automatically provide implementations for common methods
    // like save(), findById(), findAll(), delete(), etc.
}
