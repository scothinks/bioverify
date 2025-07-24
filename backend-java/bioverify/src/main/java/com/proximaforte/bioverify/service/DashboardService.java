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

        // --- CORRECTED CALCULATION FOR 'totalVerified' ---
        // "Verified" means a record has passed the SoT check. This includes records
        // that are pending final approval AND those that are fully validated.
        List<RecordStatus> verifiedStatuses = List.of(RecordStatus.PENDING_GRADE_VALIDATION, RecordStatus.VALIDATED);
        long totalVerified = recordRepository.countByTenantIdAndStatusIn(tenantId, verifiedStatuses);
        
        // --- Fetch other counts from the repositories ---
        long totalUniqueRecords = recordRepository.countByTenantId(tenantId);
        long totalValidated = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.VALIDATED);
        long totalPendingApproval = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.PENDING_GRADE_VALIDATION);
        long totalMismatched = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_DATA_MISMATCH);
        long totalNotFound = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.FLAGGED_NOT_IN_SOT);
        long totalAwaitingReVerification = recordRepository.countByTenantIdAndStatus(tenantId, RecordStatus.AWAITING_REVALIDATION);
        
        long totalReviewers = userRepository.countByTenantIdAndRole(tenantId, Role.REVIEWER);
        long totalSelfServiceUsers = userRepository.countByTenantIdAndRole(tenantId, Role.SELF_SERVICE_USER);
        long totalAgentAccounts = userRepository.countByTenantIdAndRole(tenantId, Role.AGENT);

        // --- Build and return the DTO ---
        return DashboardStatsDto.builder()
                .totalUniqueRecords(totalUniqueRecords)
                .totalVerified(totalVerified) // This is now correct
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