package com.brex.demo.security;

import com.brex.demo.model.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String PAYMENT_NAME_CLAIM = "paymentName";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = deriveSigningKey(secret);
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Derives a fixed 256-bit HMAC key from the configured secret, whatever
     * its length, so a short JWT_SECRET can't cause a WeakKeyException at
     * startup while still being deterministic across restarts.
     */
    private static SecretKey deriveSigningKey(String secret) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public String generateToken(UserProfile user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(PAYMENT_NAME_CLAIM, user.getPaymentName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * @throws JwtException if the token is missing, malformed, expired, or
     *         has an invalid signature.
     */
    public AuthenticatedUser parseAuthenticatedUser(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        String paymentName = claims.get(PAYMENT_NAME_CLAIM, String.class);
        return new AuthenticatedUser(userId, paymentName);
    }
}
