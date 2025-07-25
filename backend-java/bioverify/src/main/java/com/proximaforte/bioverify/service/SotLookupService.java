package com.proximaforte.bioverify.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximaforte.bioverify.domain.Tenant;
import com.proximaforte.bioverify.dto.IdentitySourceConfigDto;
import com.proximaforte.bioverify.dto.SotProfileDto;
import com.proximaforte.bioverify.exception.TenantConfigurationException;
import com.proximaforte.bioverify.exception.TenantNotFoundException;
import com.proximaforte.bioverify.repository.TenantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SotLookupService {

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @SneakyThrows
    public Mono<SotProfileDto> getProfile(UUID tenantId, String ssid, String nin) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Configuration for tenant " + tenantId + " not found."));

        String jsonConfig = tenant.getIdentitySourceConfig();
        if (jsonConfig == null || jsonConfig.isBlank()) {
            throw new TenantConfigurationException("SoT API configuration is missing for tenant " + tenantId);
        }

        IdentitySourceConfigDto config = objectMapper.readValue(jsonConfig, IdentitySourceConfigDto.class);

        if ("OPTIMA".equalsIgnoreCase(config.getProviderName())) {
            return getProfileFromOptimaProvider(config, nin, ssid);
        } else {
            return getProfileWithOAuth2(tenant.getId().toString(), config, nin, ssid);
        }
    }

    private Mono<SotProfileDto> getProfileFromOptimaProvider(IdentitySourceConfigDto config, String nin, String ssid) {
        WebClient webClient = webClientBuilder.build();
        String baseUrl = config.getApiBaseUrl();

        // Step 1: Call /encrypt
        Map<String, String> initialPayload = Map.of("nin", nin, "ssid", ssid);
        return webClient.post()
            .uri(baseUrl + "/encrypt")
            .header("client-id", config.getClientId()) // **FIXED: Changed header to match Postman**
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(initialPayload)
            .retrieve()
            .bodyToMono(ApiResponseWrapper.class)
            .flatMap(encryptResponse -> {
                // Step 2: Call /data-inquiry
                String encryptedRequestPayload = encryptResponse.getData();
                return webClient.post()
                    .uri(baseUrl + "/data-inquiry")
                    .header("client-id", config.getClientId()) // **FIXED: Changed header to match Postman**
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(encryptedRequestPayload)
                    .retrieve()
                    .bodyToMono(ApiResponseWrapper.class);
            })
            .flatMap(inquiryResponse -> {
                // Step 3: Call /decrypt
                String encryptedResponsePayload = inquiryResponse.getData();
                return webClient.post()
                    .uri(baseUrl + "/decrypt")
                    .header("client-id", config.getClientId()) // **FIXED: Changed header to match Postman**
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(encryptedResponsePayload)
                    .retrieve()
                    .bodyToMono(ApiResponseWrapper.class);
            })
            .map(decryptResponse -> {
                // Final Step: Parse the decrypted JSON
                try {
                    String finalJson = decryptResponse.getData();
                    return objectMapper.readValue(finalJson, SotProfileDto.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse final decrypted response", e);
                }
            });
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ApiResponseWrapper {
        private String data;
    }
    
    private Mono<SotProfileDto> getProfileWithOAuth2(String registrationId, IdentitySourceConfigDto config, String nin, String ssid) {
        WebClient webClient = buildOAuthClientForConfig(registrationId, config);
        String lookupUrl = config.getApiBaseUrl() + "/v1/identities/lookup";
        return webClient.get()
                .uri(lookupUrl, uriBuilder -> uriBuilder
                        .queryParam("ssid", ssid)
                        .queryParam("nin", nin)
                        .build())
                .retrieve()
                .bodyToMono(SotProfileDto.class);
    }

    private WebClient buildOAuthClientForConfig(String registrationId, IdentitySourceConfigDto config) {
        String decryptedSecret = encryptionService.decrypt(config.getClientSecretEncrypted());
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(registrationId)
                .clientId(config.getClientId())
                .clientSecret(decryptedSecret)
                .tokenUri(config.getTokenUri())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("api.read")
                .build();
        InMemoryClientRegistrationRepository registrationRepository = new InMemoryClientRegistrationRepository(clientRegistration);
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(registrationRepository, null);
        return webClientBuilder.build().mutate()
                .apply(oauth2Filter.oauth2Configuration())
                .build();
    }
}