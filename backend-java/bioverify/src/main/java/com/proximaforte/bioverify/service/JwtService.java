package com.proximaforte.bioverify.service;

import com.proximaforte.bioverify.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for handling JSON Web Tokens (JWTs).
 * This includes generating, validating, and extracting information from tokens.
 */
@Service
public class JwtService {

    // The secret key used to sign the JWTs. This should be a long, random string.
    // We will load it from our application.properties file.
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // The expiration time for the JWTs in milliseconds.
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extracts the username (email) from a JWT.
     * @param token The JWT.
     * @return The username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from a JWT.
     * @param token The JWT.
     * @param claimsResolver A function to extract the desired claim.
     * @return The claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT for a given user.
     * @param userDetails The user details.
     * @return The generated JWT.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Add custom claims if the user is an instance of our User class
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            extraClaims.put("role", user.getRole().name());
            if (user.getTenant() != null) {
                extraClaims.put("tenantId", user.getTenant().getId().toString());
            }
        }
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Checks if a JWT is valid.
     * @param token The JWT.
     * @param userDetails The user details to validate against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
