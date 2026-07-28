package com.soundshelf.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    public static final String ITUNES_CACHE = "itunesCatalog";

    /**
     * iTunes throttles roughly 20 calls per minute per IP and has no API key, so a
     * short in-memory cache is what keeps a debounced search box from tripping it.
     * Ten minutes is well inside how often the public catalog actually changes.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(ITUNES_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(500));
        return manager;
    }
}
