package com.proximaforte.bioverify.domain;

import com.proximaforte.bioverify.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payroll_export_logs")
public class PayrollExportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exported_by_user_id")
    private User exportedBy;

    @Column(nullable = false)
    private Instant exportTimestamp;

    @Column(nullable = false)
    private Long recordCount;

    @Column
    private String fileUrl;

    /**
     * The final status of the export job (e.g., RUNNING, COMPLETED, FAILED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    /**
     * A message field, typically used to store an error message if the job fails.
     */
    @Column(columnDefinition = "TEXT")
    private String statusMessage;
}