package com.gamingbar.service.impl;

import com.gamingbar.cache.CacheService;
import com.gamingbar.common.util.JwtUtils;
import com.gamingbar.service.TokenBlacklistService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String PREFIX = "auth:blacklist:";

    private final CacheService cacheService;
    private final JwtUtils jwtUtils;

    public TokenBlacklistServiceImpl(CacheService cacheService, JwtUtils jwtUtils) {
        this.cacheService = cacheService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void blacklist(String token) {
        Instant expireAt = jwtUtils.parseExpireAt(token);
        Duration ttl = Duration.between(Instant.now(), expireAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        cacheService.set(PREFIX + digest(token), "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return cacheService.get(PREFIX + digest(token)) != null;
    }

    private String digest(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
