package com.brex.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brex.demo.model.UserProfile;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private UserProfile testUser() {
        UserProfile user = new UserProfile("Test", "User", "test@example.com", "555-0100", "testuser", "hash");
        user.setId(42L);
        return user;
    }

    @Test
    void generatedTokenRoundTripsToTheSameUser() {
        JwtService jwtService = new JwtService("test-secret-for-unit-tests-only", 60);
        String token = jwtService.generateToken(testUser());

        AuthenticatedUser resolved = jwtService.parseAuthenticatedUser(token);

        assertEquals(42L, resolved.userId());
        assertEquals("testuser", resolved.paymentName());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService("test-secret-for-unit-tests-only", -1);
        String token = jwtService.generateToken(testUser());

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseAuthenticatedUser(token));
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService issuer = new JwtService("secret-one", 60);
        JwtService verifier = new JwtService("secret-two", 60);
        String token = issuer.generateToken(testUser());

        assertThrows(io.jsonwebtoken.security.SignatureException.class,
                () -> verifier.parseAuthenticatedUser(token));
    }
}
