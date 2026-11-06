package org.example.pass.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

// Подлинность билета доказывает подпись, поэтому своя БД не нужна.
// Секрет отдельный от jwt.secret: токен билета лежит в PDF и не должен подходить для входа в систему
@Component
public class PassTokens {

    private static final String AUDIENCE = "ticket-pass";

    private final SecretKey key;
    private final JwtParser parser;

    public PassTokens(@Value("${ticket-pass.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(key).requireAudience(AUDIENCE).build();
    }

    // Без даты выпуска: токен одной регистрации всегда один и тот же, QR не меняется при повторном скачивании
    public String issue(Long registrationId, LocalDate eventEndDate) {
        return Jwts.builder()
                .subject(registrationId.toString())
                .audience().add(AUDIENCE).and()
                .expiration(Date.from(eventEndDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .signWith(key)
                .compact();
    }

    // id регистрации; пусто, если подпись неверна или срок истек
    public Optional<Long> verify(String token) {
        try {
            return Optional.of(Long.valueOf(parser.parseSignedClaims(token).getPayload().getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
