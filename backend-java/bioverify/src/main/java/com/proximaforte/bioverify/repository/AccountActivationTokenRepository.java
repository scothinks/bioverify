package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.proximaforte.bioverify.domain.User;

import java.util.Optional;

@Repository
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByToken(String token);
    Optional<AccountActivationToken> findByUser(User user);
}