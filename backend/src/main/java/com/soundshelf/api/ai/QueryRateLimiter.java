package com.soundshelf.api.ai;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Per-user cap on model calls. In-memory on purpose: the app runs as a single
 * instance, and the point is to stop one account burning through an API budget,
 * not to be a distributed quota system. Behind more than one instance this would
 * need to move to Redis.
 */
@Component
public class QueryRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<Long, Deque<Instant>> history = new ConcurrentHashMap<>();

    public void check(Long userId) {
        Instant cutoff = Instant.now().minus(WINDOW);
        Deque<Instant> timestamps = history.computeIfAbsent(userId, key -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new TooManyQueriesException(
                        "You have hit the limit of " + MAX_REQUESTS + " questions per minute. Try again shortly.");
            }
            timestamps.addLast(Instant.now());
        }
    }

    public static class TooManyQueriesException extends RuntimeException {
        public TooManyQueriesException(String message) {
            super(message);
        }
    }
}
