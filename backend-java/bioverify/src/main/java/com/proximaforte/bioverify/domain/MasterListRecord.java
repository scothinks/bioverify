package com.proximaforte.bioverify.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a single record in a tenant's master list.
 * This will be populated by file uploads.
 */
@Entity
@Table(name = "master_list_records")
public class MasterListRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Foreign key to the Tenant this record belongs to.
     * This is the core of our multi-tenancy strategy.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // We will add this later when we build the Employee ID generation logic.
    // @Column(unique = true)
    // private String employeeId;

    /**
     * SSID is optional at upload but required for verification.
     * It will be encrypted in a future step.
     */
    @Column
    private String ssid;

    /**
     * NIN will be collected during verification.
     * It will be encrypted.
     */
    @Column
    private String nin;

    /**
     * Full name of the employee/pensioner.
     * This will be encrypted.
     */
    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String businessUnit;

    @Column
    private String gradeLevel;

    @Column(nullable = false)
    private String status = "UPLOADED"; // Default status on creation

    private LocalDate lastProofOfLifeDate;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getNin() {
        return nin;
    }

    public void setNin(String nin) {
        this.nin = nin;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getLastProofOfLifeDate() {
        return lastProofOfLifeDate;
    }

    public void setLastProofOfLifeDate(LocalDate lastProofOfLifeDate) {
        this.lastProofOfLifeDate = lastProofOfLifeDate;
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
