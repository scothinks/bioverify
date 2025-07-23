package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service dedicated to generating unique, sequential, and permanent identifiers for employees.
 * This service relies on tenant-specific database sequences to ensure uniqueness and prevent race conditions.
 *
 * IMPORTANT: For each tenant, a corresponding database sequence must be created manually.
 * For example, for a tenant with state code 'KW', the following SQL should be executed:
 * CREATE SEQUENCE kw_employee_id_seq START 1;
 */
@Service
public class EmployeeIdService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generates a new, unique Work ID for an employee within a given tenant.
     *
     * @param tenant The tenant for whom the ID is being generated.
     * @return A formatted, unique Work ID string (e.g., "KW-WID-0000001").
     */
    @Transactional
    public String generateNewWorkId(Tenant tenant) {
        if (tenant == null || tenant.getStateCode() == null || tenant.getStateCode().isBlank()) {
            throw new IllegalArgumentException("Tenant and stateCode must not be null or blank.");
        }

        String tenantStateCode = tenant.getStateCode().toLowerCase();
        String sequenceName = tenantStateCode + "_employee_id_seq";

        // --- CORRECTED THIS BLOCK ---
        // The native query returns a Long (or more generically, a Number), not a BigInteger.
        // We cast to Number and get its long value for robust handling.
        Number sequenceResult = (Number) entityManager
                .createNativeQuery("SELECT nextval('" + sequenceName + "')")
                .getSingleResult();
        Long sequenceValue = sequenceResult.longValue();

        // The "%S" specifier converts the state code to uppercase.
        // The "%07d" specifier ensures the number is zero-padded to 7 digits.
        return String.format("%S-WID-%07d", tenantStateCode, sequenceValue);
    }
}