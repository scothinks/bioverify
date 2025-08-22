package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Department;
import com.proximaforte.bioverify.domain.Ministry;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.CreateUserRequest;
import com.proximaforte.bioverify.dto.DepartmentDto;
import com.proximaforte.bioverify.dto.MinistryDto;
import com.proximaforte.bioverify.dto.ReviewerDataDto;
import com.proximaforte.bioverify.repository.DepartmentRepository;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.MinistryRepository;
import com.proximaforte.bioverify.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MasterListRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinistryRepository ministryRepository;
    private final DepartmentRepository departmentRepository;

    public List<User> getUsersForTenant(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId);
    }
    
    @Transactional(readOnly = true)
    public List<ReviewerDataDto> getReviewersForTenant(UUID tenantId) {
        List<User> reviewers = userRepository.findByTenantIdAndRole(tenantId, Role.REVIEWER);
        
        // Count records awaiting review for workload distribution
        List<RecordStatus> statusesToCount = List.of(RecordStatus.AWAITING_REVIEW, RecordStatus.FLAGGED_DATA_MISMATCH);

        return reviewers.stream().map(reviewer -> {
            long count = 0;
            User reviewerWithAssignments = userRepository.findUserWithAssignments(reviewer.getId()).orElse(reviewer);

            if (!reviewerWithAssignments.getAssignedMinistries().isEmpty() || !reviewerWithAssignments.getAssignedDepartments().isEmpty()) {
                count = recordRepository.findRecordsByReviewerAssignments(
                    tenantId,
                    statusesToCount,
                    reviewerWithAssignments.getAssignedDepartments(),
                    reviewerWithAssignments.getAssignedMinistries()
                ).size();
            }
            
            Set<MinistryDto> ministryDtos = reviewerWithAssignments.getAssignedMinistries().stream()
                    .map(MinistryDto::new)
                    .collect(Collectors.toSet());
            Set<DepartmentDto> departmentDtos = reviewerWithAssignments.getAssignedDepartments().stream()
                    .map(DepartmentDto::new)
                    .collect(Collectors.toSet());

            return new ReviewerDataDto(
                reviewer.getId(),
                reviewer.getFullName(),
                reviewer.getEmail(),
                count,
                ministryDtos,
                departmentDtos
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public User createUser(CreateUserRequest request, Tenant tenant) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("A user with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setTenant(tenant);

        return userRepository.save(user);
    }
    
    @Transactional
    public User updateReviewerAssignments(UUID reviewerId, List<UUID> ministryIds, List<UUID> departmentIds) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found with ID: " + reviewerId));

        if (reviewer.getRole() != Role.REVIEWER) {
            throw new IllegalStateException("User is not a reviewer.");
        }

        Set<Ministry> ministries = new HashSet<>(ministryRepository.findAllById(ministryIds));
        Set<Department> departments = new HashSet<>(departmentRepository.findAllById(departmentIds));

        reviewer.setAssignedMinistries(ministries);
        reviewer.setAssignedDepartments(departments);

        return userRepository.save(reviewer);
    }
}