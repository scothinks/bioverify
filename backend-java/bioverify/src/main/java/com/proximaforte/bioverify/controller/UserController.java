package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.CreateUserRequest; // 
import com.proximaforte.bioverify.dto.MasterListRecordDto;
import com.proximaforte.bioverify.dto.UserDto; 
import com.proximaforte.bioverify.service.UserService; 
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*; 

import java.util.List; 
import java.util.stream.Collectors; 

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService; 

    @GetMapping("/me/record")
    public ResponseEntity<MasterListRecordDto> getCurrentUserRecord(@AuthenticationPrincipal User user) {
        MasterListRecord record = user.getMasterListRecord();
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new MasterListRecordDto(record));
    }

    /**
     * Creates a new administrative user (e.g., FOCAL_OFFICER).
     * Only accessible by Tenant Admins.
     */
    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request, @AuthenticationPrincipal User currentUser) {
        User newUser = userService.createUser(request, currentUser.getTenant());
        return ResponseEntity.ok(new UserDto(newUser));
    }

    /**
     * Fetches all users for the current user's tenant.
     * Only accessible by Tenant Admins.
     */
    @GetMapping 
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersForTenant(@AuthenticationPrincipal User currentUser) {
        List<User> users = userService.getUsersForTenant(currentUser.getTenant().getId());
        List<UserDto> userDtos = users.stream().map(UserDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }
}