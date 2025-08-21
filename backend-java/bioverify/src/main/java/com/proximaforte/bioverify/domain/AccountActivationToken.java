package com.proximaforte.bioverify.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "account_activation_tokens")
public class AccountActivationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    public AccountActivationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        // Set expiry to 30 minutes from now, as per the template's requirement
        this.expiryDate = Instant.now().plusSeconds(1800); 
    }

    public AccountActivationToken() {}
}