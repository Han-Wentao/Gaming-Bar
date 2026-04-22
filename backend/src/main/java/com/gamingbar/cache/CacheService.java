package com.gamingbar.cache;

import java.time.Duration;

public interface CacheService {

    void set(String key, String value, Duration ttl);

    String get(String key);

    String getAndDelete(String key);

    void delete(String key);

    long increment(String key, Duration ttl);
}
