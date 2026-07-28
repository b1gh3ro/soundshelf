package com.soundshelf.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "soundshelf")
public record AppProperties(Jwt jwt, Cors cors, Itunes itunes, Ai ai) {

    public record Jwt(String secret, Duration ttl, String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Itunes(String baseUrl, Duration timeout, String country) {
    }

    public record Ai(String apiKey, String model, Duration timeout) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
