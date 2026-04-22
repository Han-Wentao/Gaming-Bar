package com.gamingbar.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private static final String SESSION_VERSION_CLAIM = "sv";

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(Long userId, String sessionVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim(SESSION_VERSION_CLAIM, sessionVersion)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expireSeconds)))
            .signWith(secretKey)
            .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject() == null ? null : Long.parseLong(claims.getSubject());
    }

    public Instant parseExpireAt(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public String parseSessionVersion(String token) {
        return parseClaims(token).get(SESSION_VERSION_CLAIM, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
