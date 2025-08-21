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

import java.util.List;
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

        // --- UPDATED METRICS BASED ON NEW STATUSES ---

        // "Verified" means a record has passed the SoT check and is in some stage of review or is active.
        List<RecordStatus> verifiedStatuses = List.of(
            RecordStatus.AWAITING_REVIEW, 
            RecordStatus.REVIEWED, 
            RecordStatus.ACTIVE,
            RecordStatus.INACTIVE, // Still considered verified
            RecordStatus.FLAGGED_INVALID_DOCUMENT // Still considered verified
        );
        long totalVerified = recordRepository.countByTenantIdAndStatusIn(tenantId, verifiedStatuses);
        
        // "Active" is the new "Validated", meaning ready for payroll.
        long totalActive = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.ACTIVE);
        
        // "Pending Review" is the new "Pending Approval".
        long totalAwaitingReview = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.AWAITING_REVIEW);
        
        long totalMismatched = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_DATA_MISMATCH);
        long totalNotFound = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_NOT_IN_SOT);
        
        long totalReviewers = userRepository.countByTenantIdAndRole(tenantId, Role.REVIEWER);
        long totalSelfServiceUsers = userRepository.countByTenantIdAndRole(tenantId, Role.SELF_SERVICE_USER);
        long totalAgentAccounts = userRepository.countByTenantIdAndRole(tenantId, Role.AGENT);

        // --- Build and return the DTO ---
        // 'totalValidated' is now 'totalActive', and 'totalPendingApproval' is 'totalAwaitingReview'.
        // 'totalAwaitingReVerification' is removed.
        return DashboardStatsDto.builder()
                .totalUniqueRecords(recordRepository.countByTenantId(tenantId))
                .totalVerified(totalVerified)
                .totalValidated(totalActive)
                .totalPendingApproval(totalAwaitingReview)
                .totalMismatched(totalMismatched)
                .totalNotFound(totalNotFound)
                .totalAwaitingReVerification(0L) // This status is removed
                .totalReviewers(totalReviewers)
                .totalSelfServiceUsers(totalSelfServiceUsers)
                .totalAgentAccounts(totalAgentAccounts)
                .build();
    }
}