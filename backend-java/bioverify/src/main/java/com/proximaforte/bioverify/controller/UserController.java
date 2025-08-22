package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.CreateUserRequest;
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.dto.ReviewerDataDto;
import com.proximaforte.bioverify.dto.UserDto;
import com.proximaforte.bioverify.exception.RecordNotFoundException;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;
    private final MasterListRecordRepository recordRepository;

    @GetMapping("/me/record")
    public ResponseEntity<MasterListRecordDto> getCurrentUserRecord(@AuthenticationPrincipal User user) {
        MasterListRecord attachedRecord = user.getMasterListRecord();
        if (attachedRecord == null) {
            return ResponseEntity.notFound().build();
        }

        // Re-fetch the record using the new query to eagerly load details
        MasterListRecord recordWithDetails = recordRepository.findByIdWithDetails(attachedRecord.getId())
            .orElseThrow(() -> new RecordNotFoundException("Could not find details for record " + attachedRecord.getId()));

        return ResponseEntity.ok(new MasterListRecordDto(recordWithDetails));
    }

    /**
     * Creates a new administrative user (e.g., FOCAL_OFFICER).
     * Only accessible by Tenant Admins.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request, @AuthenticationPrincipal User currentUser) {
        User newUser = userService.createUser(request, currentUser.getTenant());
        return ResponseEntity.ok(new UserDto(newUser));
    }

    /**
     * Fetches all users for the current user's tenant.
     * Only accessible by Tenant Admins.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersForTenant(@AuthenticationPrincipal User currentUser) {
        List<User> users = userService.getUsersForTenant(currentUser.getTenant().getId());
        List<UserDto> userDtos = users.stream().map(UserDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Fetches all reviewers and their pending counts for the tenant.
     * Only accessible by Tenant Admins.
     */
    @GetMapping("/reviewers")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<List<ReviewerDataDto>> getReviewersForTenant(@AuthenticationPrincipal User currentUser) {
        List<ReviewerDataDto> reviewers = userService.getReviewersForTenant(currentUser.getTenant().getId());
        return ResponseEntity.ok(reviewers);
    }
}