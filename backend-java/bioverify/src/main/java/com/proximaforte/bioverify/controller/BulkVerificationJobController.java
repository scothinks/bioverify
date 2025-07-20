package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.BulkJobDto;
import com.proximaforte.bioverify.repository.BulkVerificationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bulk-jobs")
@RequiredArgsConstructor
public class BulkVerificationJobController {

    private final BulkVerificationJobRepository jobRepository;

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<BulkJobDto>> getJobsForTenant(@AuthenticationPrincipal User currentUser) {
        List<BulkJobDto> jobs = jobRepository.findAllByTenantId(currentUser.getTenant().getId())
                .stream()
                .map(BulkJobDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<BulkJobDto> getJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User currentUser) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getTenant().getId().equals(currentUser.getTenant().getId())) // Ensure job belongs to tenant
                .map(job -> ResponseEntity.ok(new BulkJobDto(job)))
                .orElse(ResponseEntity.notFound().build());
    }
}