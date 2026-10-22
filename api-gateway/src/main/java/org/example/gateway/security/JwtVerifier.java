package org.example.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Токены выпускает user-service, здесь только проверка подписи и срока тем же секретом
@Component
public class JwtVerifier {

    private final JwtParser parser;

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        this.parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    public Optional<AuthUser> verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String role = claims.get("role", String.class);
            if (claims.getSubject() == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthUser(claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
