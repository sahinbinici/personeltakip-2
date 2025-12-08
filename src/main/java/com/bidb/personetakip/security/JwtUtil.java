package com.bidb.personetakip.security;

import com.bidb.personetakip.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token generation and validation.
 * Handles token creation, validation, and claims extraction.
 * 
 * Requirements: 4.3, 4.5, 9.3, 9.4
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.issuer:personnel-tracking-system}")
    private String issuer;
    
    /**
     * Generates the secret key from the configured secret string.
     * 
     * @return SecretKey for signing JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Generates a JWT token for the given user with 30-minute expiration.
     * Includes userId, tcNo, and role in the token payload.
     * 
     * @param user User entity containing user details
     * @return JWT token string
     * Requirements: 4.3 - Generate JWT with user details
     *               4.5 - 30-minute expiration time
     *               9.3 - Sign tokens with secret key
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tcNo", user.getTcNo());
        claims.put("role", user.getRole().name());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        
        return createToken(claims, user.getTcNo());
    }
    
    /**
     * Creates a JWT token with the specified claims and subject.
     * 
     * @param claims Additional claims to include in the token
     * @param subject Subject of the token (TC No)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Validates a JWT token by checking signature and expiration.
     * 
     * @param token JWT token to validate
     * @param tcNo TC No to validate against the token subject
     * @return true if token is valid, false otherwise
     * Requirements: 9.4 - Verify signature and expiration
     */
    public boolean validateToken(String token, String tcNo) {
        try {
            final String tokenTcNo = extractTcNo(token);
            return (tokenTcNo.equals(tcNo) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validates a JWT token without checking against a specific TC No.
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     * Requirements: 9.4 - Verify signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extracts the TC No (subject) from the token.
     * 
     * @param token JWT token
     * @return TC No from token
     */
    public String extractTcNo(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extracts the user ID from the token claims.
     * 
     * @param token JWT token
     * @return User ID from token
     * Requirement: 4.3 - Extract userId from token
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        
        throw new IllegalArgumentException("Invalid userId in token");
    }
    
    /**
     * Extracts the role from the token claims.
     * 
     * @param token JWT token
     * @return Role from token
     * Requirement: 4.3 - Extract role from token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    
    /**
     * Extracts the expiration date from the token.
     * 
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extracts the issued-at date from the token.
     * 
     * @param token JWT token
     * @return Issued-at date
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }
    
    /**
     * Generic method to extract a specific claim from the token.
     * 
     * @param token JWT token
     * @param claimsResolver Function to extract the desired claim
     * @param <T> Type of the claim
     * @return Extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extracts all claims from the token.
     * Validates signature and expiration during extraction.
     * 
     * @param token JWT token
     * @return All claims from the token
     * Requirements: 9.4 - Verify signature before processing
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Checks if the token has expired.
     * 
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Gets the configured expiration time in milliseconds.
     * 
     * @return Expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return expiration;
    }
}
