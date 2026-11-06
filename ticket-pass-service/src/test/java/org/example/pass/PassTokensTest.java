package org.example.pass;

import org.example.pass.service.PassTokens;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassTokensTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    private final PassTokens tokens = new PassTokens(SECRET);
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @Test
    void validTokenReturnsRegistration() {
        assertEquals(Optional.of(5L), tokens.verify(tokens.issue(5L, tomorrow)));
    }

    @Test
    void sameRegistrationGivesSameToken() {
        assertEquals(tokens.issue(5L, tomorrow), tokens.issue(5L, tomorrow));
    }

    @Test
    void worksOnLastDayOfEventButNotAfter() {
        assertTrue(tokens.verify(tokens.issue(5L, LocalDate.now())).isPresent());
        assertTrue(tokens.verify(tokens.issue(5L, LocalDate.now().minusDays(1))).isEmpty());
    }

    @Test
    void foreignSignatureIsRejected() {
        PassTokens other = new PassTokens(SECRET + "-other");
        assertTrue(tokens.verify(other.issue(5L, tomorrow)).isEmpty());
    }

    @Test
    void tamperedAndGarbageTokensAreRejected() {
        String token = tokens.issue(5L, tomorrow);
        assertTrue(tokens.verify(token.substring(0, token.length() - 4) + "abcd").isEmpty());
        assertTrue(tokens.verify("garbage").isEmpty());
    }

    @Test
    void tokenSignedWithSameKeyForOtherPurposeIsRejected() {
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String loginToken = io.jsonwebtoken.Jwts.builder().subject("5").signWith(key).compact();
        assertTrue(tokens.verify(loginToken).isEmpty());
    }
}
