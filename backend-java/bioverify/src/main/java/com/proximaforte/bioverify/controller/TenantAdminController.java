package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.dto.UserDto;
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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TenantAdminController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody RegisterRequest request, @AuthenticationPrincipal User admin) {
        request.setTenantId(admin.getTenant().getId());
        User newUser = authenticationService.register(request);
        return ResponseEntity.ok(new UserDto(newUser));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getTenantUsers(@AuthenticationPrincipal User admin) {
        List<User> users = userService.getUsersForTenant(admin.getTenant().getId());
        List<UserDto> userDtos = users.stream().map(UserDto::new).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }
}