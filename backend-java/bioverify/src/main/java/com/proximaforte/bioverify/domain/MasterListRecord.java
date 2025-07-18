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

    // --- NEW: Fields for PSN ---
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String psn;

    @Column(unique = true)
    private String psnHash;
    
    // --- Existing Fields ---
    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String ssid;

    @Column
    @Convert(converter = StringCryptoConverter.class)
    private String nin;

    @Column(nullable = false)
    @Convert(converter = StringCryptoConverter.class)
    private String fullName;

    @Column(nullable = false)
    private String businessUnit;

    @Column
    private String gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    @Column(unique = true)
    private String ssidHash;

    @Column(unique = true)
    private String ninHash;

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

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}