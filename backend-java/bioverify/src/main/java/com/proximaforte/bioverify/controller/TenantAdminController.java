package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.DepartmentDto;
import com.proximaforte.bioverify.dto.MinistryDto;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.dto.UserDto;
import com.proximaforte.bioverify.repository.DepartmentRepository;
import com.proximaforte.bioverify.repository.MinistryRepository;
import com.proximaforte.bioverify.service.AuthenticationService;
import com.proximaforte.bioverify.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
// --- UPDATED: Broadened the base path for the controller ---
@RequestMapping("/api/v1/tenant-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TenantAdminController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final MinistryRepository ministryRepository; // <-- ADDED
    private final DepartmentRepository departmentRepository; // <-- ADDED

    // --- UPDATED: Added specific path for creating users ---
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody RegisterRequest request, @AuthenticationPrincipal User admin) {
        request.setTenantId(admin.getTenant().getId());
        
        User newUser = authenticationService.register(request);
        return ResponseEntity.ok(new UserDto(newUser));
    }

    // --- UPDATED: Added specific path for getting users ---
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getTenantUsers(@AuthenticationPrincipal User admin) {
        List<User> users = userService.getUsersForTenant(admin.getTenant().getId());
        List<UserDto> userDtos = users.stream().map(UserDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    // --- NEW ENDPOINT ---
    @GetMapping("/ministries")
    public ResponseEntity<List<MinistryDto>> getMinistriesForTenant(@AuthenticationPrincipal User admin) {
        List<MinistryDto> ministries = ministryRepository.findAllByTenantId(admin.getTenant().getId())
                .stream()
                .map(MinistryDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ministries);
    }

    // --- NEW ENDPOINT ---
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsForTenant(@AuthenticationPrincipal User admin) {
        List<DepartmentDto> departments = departmentRepository.findAllByTenantId(admin.getTenant().getId())
                .stream()
                .map(DepartmentDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departments);
    }
}