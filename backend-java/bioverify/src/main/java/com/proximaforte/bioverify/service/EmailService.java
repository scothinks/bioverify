package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.dto.EmailRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class EmailService {

    private final WebClient webClient;

    @Value("${email.service.api.url}")
    private String emailServiceUrl;

    @Value("${email.service.client-id}")
    private String emailServiceClientId;

    public EmailService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void sendAccountActivationEmail(String userFullName, String userEmail, String activationLink) {
        // Build the request DTO
        EmailRequestDto emailRequest = new EmailRequestDto();
        emailRequest.setClientId(emailServiceClientId);
        emailRequest.setTemplateName("ACCOUNT_ACTIVIATION");
        emailRequest.setToEmail(userEmail);
        emailRequest.setSendEmail(true);
        emailRequest.setSendSms(false);

        // Set the dynamic parameter for the template
        emailRequest.setParameters(Map.of("activationLink", activationLink));

        // Send the request to the email service API
        webClient.post()
            .uri(emailServiceUrl)
            .body(Mono.just(emailRequest), EmailRequestDto.class)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(error -> System.err.println("Failed to send activation email: " + error.getMessage()))
            .subscribe();
    }
}