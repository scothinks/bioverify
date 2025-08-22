package com.proximaforte.bioverify.domain;

import com.proximaforte.bioverify.crypto.StringCryptoConverter;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central entity representing an employee record in the biometric verification system.
 * 
 * This entity manages the complete employee lifecycle from initial upload through
 * verification, proof of life, and final payroll eligibility. Key features include:
 * 
 * - Multi-level data encryption for PII protection
 * - Dual storage: encrypted values + searchable hashes for identifiers
 * - Workflow status tracking through RecordStatus enum
 * - Integration with external Source of Truth systems
 * - Proof of Life documentation and agent tracking
 * - Liveness check scheduling and monitoring
 * - Comprehensive audit trails
 * 
 * The entity supports multi-tenant isolation and role-based access control.
 */
@Getter
@Setter
@Entity
@Table(name = "master_list_records")
public class MasterListRecord {

    /** Primary key - UUID for enhanced security and distributed systems compatibility */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Multi-tenant isolation - each record belongs to exactly one tenant */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Optional link to user account (created after successful PoL) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // === CORE IDENTIFIERS ===
    // All sensitive identifiers are stored both encrypted and as searchable hashes
    
    /** Work ID - unique identifier assigned after validation */
    @Column(unique = true)
    private String wid;

    /** Personal Service Number - encrypted sensitive identifier */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String psn;

    /** Searchable hash of PSN for database queries */
    @Column(unique = true)
    private String psnHash;

    /** State Staff ID - encrypted sensitive identifier */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String ssid;

    /** Searchable hash of SSID for database queries */
    @Column(unique = true)
    private String ssidHash;

    /** National Identification Number - encrypted sensitive identifier */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String nin;

    /** Searchable hash of NIN for database queries */
    @Column(unique = true)
    private String ninHash;

    /** Bank Verification Number - encrypted for financial security */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String bvn;

    /** Internal employee ID - not necessarily unique across tenants */
    @Column(unique = true)
    private String employeeId;

    // === PERSONAL DETAILS ===
    // Encrypted personal information for privacy protection
    
    /** Employee full name - encrypted for PII protection */
    @Column(nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    private String fullName;

    /** Date of birth for age verification and eligibility checks */
    @Column
    private LocalDate dateOfBirth;

    /** Gender information - not encrypted as it's not considered highly sensitive */
    @Column
    private String gender;

    // === CONTACT DETAILS ===
    // Encrypted contact information
    
    /** Phone number - encrypted for privacy */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String phoneNumber;

    /** Email address - encrypted, used for account activation notifications */
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String email;

    // === CIVIL SERVICE DETAILS ===
    // Organizational structure and employment information
    
    /** Department affiliation for role-based access control */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Ministry affiliation for role-based access control */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_id")
    private Ministry ministry;

    /** Civil service grade level for payroll calculations */
    @Column
    private String gradeLevel;

    /** Salary structure category */
    @Column
    private String salaryStructure;

    /** Initial appointment date for service length calculations */
    @Column
    private LocalDate dateOfFirstAppointment;

    /** Confirmation date indicating permanent employment status */
    @Column
    private LocalDate dateOfConfirmation;

    /** Professional cadre or job category */
    @Column
    private String cadre;

    /** Transfer status flag for payroll processing considerations */
    @Column
    private Boolean onTransfer;

    // === PROOF OF LIFE & BIOMETRIC DATA ===
    // Information related to biometric verification and liveness checks
    
    /** Agent who performed the Proof of Life verification */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pol_agent_id")
    private User polAgent;

    /** Timestamp when Proof of Life was completed */
    @Column
    private Instant polPerformedAt;

    /** URL to stored employee photo from PoL session */
    @Column
    private String photoUrl;

    /** List of document URLs (Letter of Employment, Work ID, etc.) */
    @ElementCollection
    @CollectionTable(name = "record_document_urls", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "document_url")
    private List<String> documentUrls = new ArrayList<>();

    /** Date of last liveness check submission */
    @Column
    private LocalDate lastLivenessCheckDate;

    /** Scheduled date for next required liveness check */
    @Column
    private LocalDate nextLivenessCheckDate;

    // === SYSTEM & WORKFLOW STATUS ===
    // Status tracking and audit information
    
    /** Current status in the verification workflow */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    /** Biometric verification status */
    @Column
    private Boolean biometricStatus;

    /** JSON data from original CSV upload for comparison purposes */
    @Column(columnDefinition = "TEXT")
    private String originalUploadData;

    /** JSON data from Source of Truth for mismatch resolution */
    @Column(columnDefinition = "TEXT")
    private String sotData;

    /** Timestamp when record was verified against external systems */
    @Column
    private Instant verifiedAt;

    /** User who performed the final validation */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_user_id")
    private User validatedBy;

    /** Timestamp of final validation */
    @Column
    private Instant validatedAt;

    /** Reference to payroll export that included this record */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_export_log_id")
    private PayrollExportLog payrollExportLog;

    /** User who last modified this record */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_user_id")
    private User lastUpdatedBy;

    /** Automatic creation timestamp - immutable */
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    /** Automatic update timestamp - managed by Hibernate */
    @UpdateTimestamp
    private Instant updatedAt;
}