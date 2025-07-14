package com.proximaforte.bioverify.domain;

import com.proximaforte.bioverify.crypto.StringCryptoConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "master_list_records")
public class MasterListRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column
    @Convert(converter = StringCryptoConverter.class) 
    private String ssid;

    @Column
    @Convert(converter = StringCryptoConverter.class) 
    private String nin;

    @Column(nullable = false)
    @Convert(converter = StringCryptoConverter.class) 
    private String fullName;

    // Non-sensitive fields are not converted
    @Column(nullable = false)
    private String businessUnit;

    @Column
    private String gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    private LocalDate lastProofOfLifeDate;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // --- Getters and Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    public RecordStatus getStatus() { return status; }
    public void setStatus(RecordStatus status) { this.status = status; }
    public LocalDate getLastProofOfLifeDate() { return lastProofOfLifeDate; }
    public void setLastProofOfLifeDate(LocalDate lastProofOfLifeDate) { this.lastProofOfLifeDate = lastProofOfLifeDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}