package com.proximaforte.bioverify.repository;

import com.proximaforte.bioverify.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the User entity.
 * This provides CRUD operations and custom finders for the 'users' table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * Since email is our username and is unique, this is the primary method
     * for looking up users during the authentication process.
     *
     * @param email The email address to search for.
     * @return An Optional containing the found User, or an empty Optional if not found.
     */
    Optional<User> findByEmail(String email);

}
