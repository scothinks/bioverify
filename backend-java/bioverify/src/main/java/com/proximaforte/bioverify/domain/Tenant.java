package com.proximaforte.bioverify.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a Tenant in the system (e.g., Kwara State, Osun State).
 * This class is a JPA entity that maps to the "tenants" table in the database.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    /**
     * The unique identifier for the tenant.
     * It is a UUID, generated automatically when a new tenant is created.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The official name of the tenant.
     * This column cannot be null.
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * The unique subdomain for the tenant used for routing (e.g., "kwara").
     * This column must be unique and cannot be null.
     */
    @Column(nullable = false, unique = true)
    private String subdomain;

    /**
     * The official state code (e.g., "KW" for Kwara).
     * Used for generating Employee IDs.
     */
    @Column(nullable = false)
    private String stateCode;
    
    /**
     * NEW: A brief description of the tenant.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Stores the tenant-specific configuration for the Optima registry as a JSON string.
     * For a more advanced implementation, this could be mapped directly to a JSONB type.
     */
    @Column(columnDefinition = "TEXT")
    private String identitySourceConfig;

    /**
     * A flag to indicate if the tenant is active or suspended.
     */
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * The timestamp when the tenant record was created.
     * This is automatically set by Hibernate.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    /**
     * The timestamp when the tenant record was last updated.
     * This is automatically set by Hibernate on every update.
     */
    @UpdateTimestamp
    private Instant updatedAt;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdentitySourceConfig() {
        return identitySourceConfig;
    }

    public void setIdentitySourceConfig(String identitySourceConfig) {
        // CORRECTED: Fixed 'thisis' typo
        this.identitySourceConfig = identitySourceConfig;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}