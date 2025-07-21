package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.dto.CreateUserRequest;
import com.proximaforte.bioverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; 

    public List<User> getUsersForTenant(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId);
    }

    /**
     * Creates a new User, hashes their password, and assigns them to a tenant.
     * Throws an exception if the email is already in use.
     * @param request The DTO containing the new user's details.
     * @param tenant The tenant to which the new user will be assigned.
     * @return The newly created and saved User entity.
     */
    @Transactional
    public User createUser(CreateUserRequest request, Tenant tenant) {
        // Check if a user with this email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            // Or handle with a custom, more specific exception
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
}