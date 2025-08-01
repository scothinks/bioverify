package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.*;
import com.proximaforte.bioverify.domain.enums.RecordStatus;
import com.proximaforte.bioverify.domain.enums.Role;
import com.proximaforte.bioverify.dto.*;
import com.proximaforte.bioverify.exception.RecordNotFoundException;
import com.proximaforte.bioverify.exception.TokenRefreshException;
import com.proximaforte.bioverify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    // --- Injected Dependencies ---
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MasterListRecordRepository recordRepository;
    private final MinistryRepository ministryRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AccountActivationTokenRepository activationTokenRepository; // NEW
    private final EmailService emailService; // NEW

    @Value("${app.frontend.base-url}") // NEW
    private String frontendBaseUrl;

    // --- Existing Methods (Unchanged) ---
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

        if (record.getStatus() != RecordStatus.AWAITING_REVIEW && record.getStatus() != RecordStatus.REVIEWED) {
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

    /**
     * UPDATED: Now generates an activation token and sends the activation email.
     */
    @Transactional
    public User createSelfServiceAccountForRecord(MasterListRecord record, String verifiedEmail) {
        if (record.getUser() != null) {
            throw new IllegalStateException("An account has already been created for this employee record.");
        }
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new IllegalStateException("Cannot create account: A valid email address provided by the agent is required.");
        }

        String temporaryPassword = UUID.randomUUID().toString();
        User user = new User();
        user.setFullName(record.getFullName());
        user.setEmail(verifiedEmail);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRole(Role.SELF_SERVICE_USER);
        user.setTenant(record.getTenant());
        user.setEnabled(false); // User is not enabled until they activate

        User savedUser = userRepository.save(user);

        // --- NEW ACTIVATION LOGIC ---
        // 1. Create and save the activation token
        AccountActivationToken activationToken = new AccountActivationToken(savedUser);
        activationTokenRepository.save(activationToken);

        // 2. Build the activation link
        String activationLink = frontendBaseUrl + "/activate-account?token=" + activationToken.getToken();

        // 3. Send the email
        emailService.sendAccountActivationEmail(savedUser.getFullName(), savedUser.getEmail(), activationLink);
        
        return savedUser;
    }

    /**
     * NEW: Method to activate a user account with a new password.
     */
    @Transactional
    public void activateAccount(String token, String password) {
        AccountActivationToken activationToken = activationTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("Invalid activation token."));

        if (activationToken.getExpiryDate().isBefore(Instant.now())) {
            activationTokenRepository.delete(activationToken);
            throw new BadCredentialsException("Activation token has expired.");
        }

        User user = activationToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true); // Enable the user's account
        userRepository.save(user);

        // Delete the token so it cannot be used again
        activationTokenRepository.delete(activationToken);
    }
    
    // --- Authentication and Refresh Token Methods (Unchanged) ---
    public JwtResponse authenticate(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();
        String jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        return new JwtResponse(
                jwtToken,
                refreshToken.getToken(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    /**
     * NEW: Resends an activation link to a user whose account is not yet enabled.
     */
    @Transactional
    public void resendActivationLink(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("No account found with that email address."));

        if (user.isEnabled()) {
            throw new IllegalStateException("This account has already been activated.");
        }

        // Delete any old token and create a new one
        activationTokenRepository.findByUser(user).ifPresent(activationTokenRepository::delete);
        AccountActivationToken newActivationToken = new AccountActivationToken(user);
        activationTokenRepository.save(newActivationToken);

        // Build the new link and send the email
        String activationLink = frontendBaseUrl + "/activate-account?token=" + newActivationToken.getToken();
        emailService.sendAccountActivationEmail(user.getFullName(), user.getEmail(), activationLink);
    }
}