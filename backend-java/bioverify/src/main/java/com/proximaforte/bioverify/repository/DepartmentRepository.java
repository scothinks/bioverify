package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByNameAndTenantId(String name, UUID tenantId);
    List<Department> findAllByTenantId(UUID tenantId);
}