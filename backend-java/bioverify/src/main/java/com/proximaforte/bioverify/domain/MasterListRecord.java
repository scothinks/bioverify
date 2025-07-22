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
import java.util.UUID;

@Getter
@Setter
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // --- Core Identifiers ---
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String psn;

    @Column(unique = true)
    private String psnHash;

    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String ssid;

    @Column(unique = true)
    private String ssidHash;

    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String nin;

    @Column(unique = true)
    private String ninHash;

    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String bvn;

    // --- Personal Details ---
    @Column(nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    private String fullName;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String gender;

    // --- Contact Details ---
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String phoneNumber;

    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String email;

    // --- Civil Service Details ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_id")
    private Ministry ministry;

    @Column
    private String gradeLevel;

    @Column
    private String salaryStructure;

    @Column
    private LocalDate dateOfFirstAppointment;

    @Column
    private LocalDate dateOfConfirmation;

    @Column
    private String cadre;

    @Column
    private Boolean onTransfer;

    // --- System & Status Fields ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    @Column
    private Boolean biometricStatus;

    private LocalDate lastProofOfLifeDate;

    @Column(columnDefinition = "TEXT")
    private String originalUploadData;

    @Column(columnDefinition = "TEXT")
    private String sotData;

    @Column
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_user_id")
    private User validatedBy;

    @Column
    private Instant validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_user_id")
    private User lastUpdatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}