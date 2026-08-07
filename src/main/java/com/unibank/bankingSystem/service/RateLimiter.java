package com.unibank.bankingSystem.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple in-memory, per-key cooldown limiter.
 *
 * Not distributed - state lives in this instance's memory, so it resets on restart and
 * isn't shared across multiple app instances. That's an acceptable tradeoff here: the
 * goal is to stop a single caller from hammering an endpoint like forgot-password to
 * flood one inbox, not to enforce a hard global quota.
 */
@Component
public class RateLimiter {

    private final ConcurrentMap<String, Instant> lastAllowedAt = new ConcurrentHashMap<>();

    /**
     * Returns true and records this instant as the latest allowed attempt for {@code key}
     * if at least {@code cooldown} has elapsed since the last allowed attempt (or if there
     * was none). Otherwise returns false and leaves the recorded time untouched.
     */
    public boolean allow(String key, Duration cooldown) {
        Instant now = Instant.now();
        boolean[] allowed = new boolean[1];

        lastAllowedAt.compute(key, (k, previous) -> {
            if (previous == null || Duration.between(previous, now).compareTo(cooldown) >= 0) {
                allowed[0] = true;
                return now;
            }
            allowed[0] = false;
            return previous;
        });

        return allowed[0];
    }
}
