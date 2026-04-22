package com.gamingbar.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.cache.provider", havingValue = "in-memory")
public class InMemoryCacheService implements CacheService {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, expiresAt(ttl)));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public String getAndDelete(String key) {
        cleanupExpired(key);
        Entry removed = store.remove(key);
        return removed == null ? null : removed.value();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public long increment(String key, Duration ttl) {
        cleanupExpired(key);
        Entry updated = store.compute(key, (ignored, current) -> {
            long nextValue = current == null ? 1L : Long.parseLong(current.value()) + 1L;
            return new Entry(String.valueOf(nextValue), expiresAt(ttl));
        });
        return Long.parseLong(Objects.requireNonNull(updated).value());
    }

    private void cleanupExpired(String key) {
        Entry entry = store.get(key);
        if (entry != null && entry.expired()) {
            store.remove(key, entry);
        }
    }

    private Instant expiresAt(Duration ttl) {
        return Instant.now().plus(ttl);
    }

    private record Entry(String value, Instant expiresAt) {
        private boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
