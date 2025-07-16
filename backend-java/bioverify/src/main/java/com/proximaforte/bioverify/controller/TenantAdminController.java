package com.proximaforte.bioverify.controller;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.AdminCreateUserRequest;
import com.proximaforte.bioverify.dto.UserDto;
import com.proximaforte.bioverify.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant-admin")
public class TenantAdminController {

    private final AuthenticationService authenticationService;

    public TenantAdminController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Endpoint for a Tenant Admin to create a new user (e.g., an Enumerator).
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody AdminCreateUserRequest request, Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        User newUser = authenticationService.createUserByAdmin(request, admin);
        UserDto userDto = new UserDto(newUser);
        return ResponseEntity.ok(userDto);
    }

    /**
     * NEW: Endpoint for a Tenant Admin to get all users in their tenant.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getTenantUsers(Authentication authentication) {
        User admin = (User) authentication.getPrincipal();
        if (admin.getTenant() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<User> users = authenticationService.getUsersForTenant(admin.getTenant().getId());
        
        // Convert the list of User entities to a list of safe DTOs
        List<UserDto> userDtos = users.stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(userDtos);
    }
}