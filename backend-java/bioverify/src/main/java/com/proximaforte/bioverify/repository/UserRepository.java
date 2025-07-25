package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findAllByTenantId(UUID tenantId);
    
    // NEW: Method to find users by tenant and role
    List<User> findByTenantIdAndRole(UUID tenantId, Role role);

    @Query("SELECT u FROM User u JOIN FETCH u.tenant WHERE u.id = :userId")
    Optional<User> findByIdWithTenant(@Param("userId") UUID userId);
    
    // NEW: Method to eagerly fetch a user's assignments to prevent LazyInitializationException
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.assignedMinistries " +
           "LEFT JOIN FETCH u.assignedDepartments " +
           "WHERE u.id = :userId")
    Optional<User> findUserWithAssignments(@Param("userId") UUID userId);

    // --- NEW METHOD FOR DASHBOARD STATS ---
    /**
     * Counts the number of users for a tenant that have a specific role.
     * This will be used for counting Reviewers, Agents, and Self-Service Users.
     */
    long countByTenantIdAndRole(UUID tenantId, Role role);
}