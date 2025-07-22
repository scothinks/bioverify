package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.Ministry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MinistryRepository extends JpaRepository<Ministry, UUID> {
    Optional<Ministry> findByNameAndTenantId(String name, UUID tenantId);
    List<Ministry> findAllByTenantId(UUID tenantId);
}