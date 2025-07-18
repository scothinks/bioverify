package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getUsersForTenant(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId);
    }
}