package com.proximaforte.bioverify.dto;

import lombok.Builder;
import lombok.Data;

/**
 * A DTO to hold the aggregated statistics for the Tenant Admin performance dashboard.
 */
@Data
@Builder
public class DashboardStatsDto {
    private long totalUniqueRecords;
    private long totalVerified;
    private long totalValidated;
    private long totalPendingApproval;
    private long totalMismatched;
    private long totalNotFound;
    private long totalAwaitingReVerification;
    private long totalReviewers;
    private long totalSelfServiceUsers;
    private long totalAgentAccounts;
}