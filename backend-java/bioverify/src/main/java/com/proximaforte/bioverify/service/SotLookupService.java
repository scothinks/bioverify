package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.SotProfileDto; 
import com.proximaforte.bioverify.exception.TenantConfigurationException;
import com.proximaforte.bioverify.exception.TenantNotFoundException;
import com.proximaforte.bioverify.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * A service to perform lookups against a tenant's configured Source of Truth (SoT) provider.
 * This service dynamically configures an OAuth 2.0 client for each request based on tenant-specific
 * settings stored in the database.
 */
@Service
@RequiredArgsConstructor
public class SotLookupService {

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper; 

    /**
     * Fetches a user's profile from the correct SoT provider based on the tenantId.
     *
     * @param tenantId The ID of the tenant whose SoT we need to call.
     * @param ssid     The user's State Security ID.
     * @param nin      The user's National Identity Number.
     * @return A Mono containing the user's profile DTO.
     */
    @SneakyThrows // A Lombok annotation to handle the checked exception from objectMapper.readValue
    public Mono<SotProfileDto> getProfile(UUID tenantId, String ssid, String nin) {
        // Step 1: Fetch the tenant entity from the database.
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Configuration for tenant " + tenantId + " not found."));

        String jsonConfig = tenant.getidentitySourceConfig();
        if (jsonConfig == null || jsonConfig.isBlank()) {
            throw new TenantConfigurationException("SoT API configuration is missing for tenant " + tenantId);
        }

        // Step 2: Parse the configuration from the JSON string into our DTO.
        IdentitySourceConfigDto config = objectMapper.readValue(jsonConfig, IdentitySourceConfigDto.class);

        // Step 3: Build a WebClient IN REAL-TIME using the config from the DTO.
        WebClient webClient = buildOAuthClientForConfig(tenant.getId().toString(), config);

        // Step 4: Make the API call to the tenant's specific API endpoint.
        String lookupUrl = config.getApiBaseUrl() + "/v1/identities/lookup";
        return webClient.get()
                .uri(lookupUrl, uriBuilder -> uriBuilder
                        .queryParam("ssid", ssid)
                        .queryParam("nin", nin)
                        .build())
                .retrieve()
                .bodyToMono(SotProfileDto.class);
    }

    /**
     * Constructs a WebClient instance on-the-fly, configured for OAuth 2.0 client credentials.
     *
     * @param registrationId A unique ID for the client registration (we use the tenant's UUID).
     * @param config         The DTO containing the tenant's specific API credentials and URLs.
     * @return A fully configured WebClient ready to make authenticated requests.
     */
    private WebClient buildOAuthClientForConfig(String registrationId, IdentitySourceConfigDto config) {
        // Decrypt the sensitive client secret retrieved from the database.
        String decryptedSecret = encryptionService.decrypt(config.getClientSecretEncrypted());

        // Create a unique ClientRegistration for this specific tenant and request.
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId(config.getClientId())
                .clientSecret(decryptedSecret)
                .tokenUri(config.getTokenUri())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("api.read") // Example scope, can also be part of the config DTO
                .build();

        // Create a repository for this single, in-memory registration.
        InMemoryClientRegistrationRepository registrationRepository = new InMemoryClientRegistrationRepository(clientRegistration);
        
        // This filter function handles the token exchange process automatically.
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(registrationRepository, null);

        return WebClient.builder()
                .apply(oauth2Filter.oauth2Configuration())
                .build();
    }
}