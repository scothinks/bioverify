package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.DashboardStatsDto;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MasterListRecordRepository recordRepository;
    private final UserRepository userRepository;

    /**
     * Gathers all necessary statistics for the tenant performance dashboard.
     * @param currentUser The user requesting the stats, used to identify the tenant.
     * @return A DTO containing all the calculated metrics.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(User currentUser) {
        UUID tenantId = currentUser.getTenant().getId();

        // --- Fetch all counts from the repositories ---
        long totalUniqueRecords = recordRepository.countByTenantId(tenantId);
        long totalVerified = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.PENDING_GRADE_VALIDATION);
        long totalValidated = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.VALIDATED);
        long totalMismatched = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_DATA_MISMATCH);
        long totalNotFound = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_NOT_IN_SOT);
        long totalAwaitingReVerification = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.AWAITING_REVALIDATION);
        
        // Note: 'totalVerified' above is actually the count of records pending final approval.
        // The business definition of "Total Verified" might need to be a sum of multiple statuses.
        // For now, we'll align with the status names.
        long totalPendingApproval = totalVerified;

        long totalReviewers = userRepository.countByTenantIdAndRole(tenantId, Role.REVIEWER);
        long totalSelfServiceUsers = userRepository.countByTenantIdAndRole(tenantId, Role.SELF_SERVICE_USER);
        long totalAgentAccounts = userRepository.countByTenantIdAndRole(tenantId, Role.AGENT);

        // --- Build and return the DTO ---
        return DashboardStatsDto.builder()
                .totalUniqueRecords(totalUniqueRecords)
                .totalVerified(totalVerified) // This is PENDING_GRADE_VALIDATION
                .totalValidated(totalValidated)
                .totalPendingApproval(totalPendingApproval)
                .totalMismatched(totalMismatched)
                .totalNotFound(totalNotFound)
                .totalAwaitingReVerification(totalAwaitingReVerification)
                .totalReviewers(totalReviewers)
                .totalSelfServiceUsers(totalSelfServiceUsers)
                .totalAgentAccounts(totalAgentAccounts)
                .build();
    }
}