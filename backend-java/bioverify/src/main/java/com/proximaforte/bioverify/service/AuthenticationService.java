package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.*;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.AuthResponse;
import com.proximaforte.bioverify.dto.AuthenticationRequest;
import com.proximaforte.bioverify.dto.CreateAccountRequest;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.exception.RecordNotFoundException;
import com.proximaforte.bioverify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MasterListRecordRepository recordRepository;
    private final MinistryRepository ministryRepository; // <-- ADDED
    private final DepartmentRepository departmentRepository; // <-- ADDED
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public User register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setTenant(tenant);

        // --- NEW LOGIC FOR ASSIGNING REVIEWERS ---
        // If the new user is a reviewer and assignments are provided, link them.
        if (request.getRole() == Role.REVIEWER) {
            if (request.getAssignedMinistryIds() != null && !request.getAssignedMinistryIds().isEmpty()) {
                Set<Ministry> ministries = new HashSet<>(ministryRepository.findAllById(request.getAssignedMinistryIds()));
                user.setAssignedMinistries(ministries);
            }
            if (request.getAssignedDepartmentIds() != null && !request.getAssignedDepartmentIds().isEmpty()) {
                Set<Department> departments = new HashSet<>(departmentRepository.findAllById(request.getAssignedDepartmentIds()));
                user.setAssignedDepartments(departments);
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public User createAccount(CreateAccountRequest request) {
        MasterListRecord record = recordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new RecordNotFoundException("Cannot create account: Master record not found."));

        if (record.getStatus() != RecordStatus.PENDING_GRADE_VALIDATION) {
            throw new IllegalStateException("Cannot create account: Record is not in a valid state for account creation.");
        }
        if (record.getUser() != null) {
            throw new IllegalStateException("An account has already been registered for this employee record.");
        }

        User user = new User();
        user.setFullName(record.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.SELF_SERVICE_USER);
        user.setTenant(record.getTenant());

        User savedUser = userRepository.save(user);
        record.setUser(savedUser);
        recordRepository.save(record);
        return savedUser;
    }

    public AuthResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}