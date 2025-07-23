package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.DashboardStatsDto;
import com.proximaforte.bioverify.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retrieves aggregated statistics for the tenant performance dashboard.
     * Accessible only by Tenant Admins.
     *
     * @param currentUser The currently authenticated user.
     * @return A DTO containing various performance metrics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(@AuthenticationPrincipal User currentUser) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(currentUser);
        return ResponseEntity.ok(stats);
    }
}