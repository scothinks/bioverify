package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.MasterListRecord;
import com.proximaforte.bioverify.domain.RecordStatus;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.domain.User;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.*;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MasterListRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;

    public AuthenticationService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            MasterListRecordRepository recordRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            VerificationService verificationService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.recordRepository = recordRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.verificationService = verificationService;
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
        String ssidHash = toSha256(request.getSsid());
        String ninHash = toSha256(request.getNin());

        MasterListRecord record = recordRepository.findBySsidHashAndNinHash(ssidHash, ninHash)
                .orElseThrow(() -> new IllegalStateException("Employee with provided SSID and NIN not found on the master list."));

        if (record.getUser() != null) {
            throw new IllegalStateException("An account has already been registered for this employee.");
        }

        Optional<Map<String, String>> trustedDataOpt = verificationService.callTenantValidationApi(request.getSsid(), request.getNin(), record.getTenant());
        if (trustedDataOpt.isEmpty()) {
            record.setStatus(RecordStatus.FLAGGED_SSID_NIN_MISMATCH);
            recordRepository.save(record);
            throw new IllegalStateException("SSID and NIN do not match the trusted registry for this tenant.");
        }

        var user = new User();
        user.setFullName(record.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.SELF_SERVICE_USER);
        user.setTenant(record.getTenant());

        var savedUser = userRepository.save(user);

        record.setUser(savedUser);
        record.setNin(request.getNin());
        record.setStatus(RecordStatus.VERIFIED_PENDING_CONFIRMATION);
        recordRepository.save(record);
        
        var jwtToken = jwtService.generateToken(savedUser);
        return new AuthenticationResponse(jwtToken);
    }

    @Transactional
    public User createUserByAdmin(AdminCreateUserRequest request, User admin) {
        Set<Role> allowedRoles = Set.of(Role.ENUMERATOR, Role.SELF_SERVICE_USER);
        if (!allowedRoles.contains(request.getRole())) {
            throw new IllegalArgumentException("Admins can only create ENUMERATOR or SELF_SERVICE_USER roles.");
        }

        if (admin.getTenant() == null) {
            throw new IllegalStateException("Admin is not associated with a tenant.");
        }

        var user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setTenant(admin.getTenant());

        return userRepository.save(user);
    }

    // --- UPDATED METHOD for Enumerator Onboarding ---
    @Transactional
    public VerificationResponse onboardEmployeeByAgent(AgentOnboardRequest request) {
        String ssidHash = toSha256(request.getSsid());
        String ninHash = toSha256(request.getNin());

        MasterListRecord record = recordRepository.findBySsidHashAndNinHash(ssidHash, ninHash)
                .orElseThrow(() -> new IllegalStateException("Employee with provided SSID and NIN not found on the master list."));

        if (record.getUser() != null) {
            throw new IllegalStateException("An account has already been registered for this employee.");
        }

        Optional<Map<String, String>> trustedDataOpt = verificationService.callTenantValidationApi(request.getSsid(), request.getNin(), record.getTenant());
        if (trustedDataOpt.isEmpty()) {
            throw new IllegalStateException("SSID and NIN do not match the trusted registry.");
        }

        // Overwrite local data with trusted data
        Map<String, String> trustedData = trustedDataOpt.get();
        record.setFullName(trustedData.get("fullName"));
        record.setGradeLevel(trustedData.get("gradeLevel"));
        record.setBusinessUnit(trustedData.get("businessUnit"));

        var user = new User();
        user.setFullName(record.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.SELF_SERVICE_USER);
        user.setTenant(record.getTenant());
        var savedUser = userRepository.save(user);

        record.setUser(savedUser);
        record.setNin(request.getNin());
        record.setStatus(RecordStatus.VERIFIED_PENDING_CONFIRMATION);
        MasterListRecord savedRecord = recordRepository.save(record);

        System.out.println("LOG: Sending welcome/password reset email to " + request.getEmail());

        // Convert to DTO to send back to the frontend for confirmation
        MasterListRecordDto recordDto = new MasterListRecordDto(
            savedRecord.getId(), savedRecord.getFullName(), savedRecord.getBusinessUnit(),
            savedRecord.getGradeLevel(), savedRecord.getStatus(), savedRecord.getCreatedAt()
        );
        
        return new VerificationResponse(true, "User account created. Please review and confirm details.", recordDto);
    }

    public List<User> getUsersForTenant(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId);
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