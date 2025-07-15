package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.RecordStatus; // Added import
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.AuthenticationRequest;
import com.proximaforte.bioverify.dto.AuthenticationResponse;
import com.proximaforte.bioverify.dto.EmployeeRegisterRequest;
import com.proximaforte.bioverify.dto.RegisterRequest;
import com.proximaforte.bioverify.repository.MasterListRecordRepository;
import com.proximaforte.bioverify.repository.TenantRepository;
import com.proximaforte.bioverify.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MasterListRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService; // <-- ADDED THIS DEPENDENCY

    public AuthenticationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            MasterListRecordRepository recordRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            VerificationService verificationService) { // <-- ADDED THIS PARAMETER
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.recordRepository = recordRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.verificationService = verificationService; // <-- INITIALIZE IT
    }

    public AuthenticationResponse register(RegisterRequest request) {
        var user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        if (request.getRole() != Role.GLOBAL_SUPER_ADMIN && request.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new IllegalStateException("Tenant not found for ID: " + request.getTenantId()));
            user.setTenant(tenant);
        }

        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }
    
    @Transactional
    public AuthenticationResponse registerEmployee(EmployeeRegisterRequest request) {
        // 1. Hash the incoming credentials to prepare for a secure lookup.
        String ssidHash = toSha256(request.getSsid());
        String ninHash = toSha256(request.getNin());

        // 2. Use the new, fast repository method to find the record instantly.
        MasterListRecord record = recordRepository.findBySsidHashAndNinHash(ssidHash, ninHash)
                .orElseThrow(() -> new IllegalStateException("Employee with provided SSID and NIN not found on the master list."));

        // 3. Check if an account already exists for this record's user.
        if (record.getUser() != null) {
            throw new IllegalStateException("An account has already been registered for this employee.");
        }

        // --- NEW: Perform external validation during registration ---
        boolean isRegistryMatch = verificationService.callTenantValidationApi(request.getSsid(), request.getNin(), record.getTenant());
        if (!isRegistryMatch) {
            record.setStatus(RecordStatus.FLAGGED_SSID_NIN_MISMATCH); // Flag if external validation fails
            recordRepository.save(record); // Save the updated status
            throw new IllegalStateException("SSID and NIN do not match the trusted registry for this tenant.");
        }
        // --- END OF NEW ---

        // 4. Create the new user with the SELF_SERVICE_USER role.
        var user = new User();
        user.setFullName(record.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.SELF_SERVICE_USER);
        user.setTenant(record.getTenant());

        var savedUser = userRepository.save(user);

        // 5. Link the new user to the master list record and set status based on successful external validation.
        record.setUser(savedUser);
        record.setNin(request.getNin()); // Save NIN if valid
        record.setStatus(RecordStatus.VERIFIED_PENDING_CONFIRMATION); // Set status after successful validation
        recordRepository.save(record);
        
        // 6. Generate and return a JWT.
        var jwtToken = jwtService.generateToken(savedUser);
        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    // Hashing utility to convert a string to its SHA-256 hash.
    private String toSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find SHA-256 algorithm", e);
        }
    }
}