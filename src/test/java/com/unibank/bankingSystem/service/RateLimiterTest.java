package com.unibank.bankingSystem.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allow_returnsTrue_onFirstAttemptForAKey() {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.allow("key", Duration.ofMinutes(10))).isTrue();
    }

    @Test
    void allow_returnsFalse_forSecondAttemptWithinCooldownWindow() {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.allow("key", Duration.ofMinutes(10))).isTrue();
        assertThat(limiter.allow("key", Duration.ofMinutes(10))).isFalse();
    }

    @Test
    void allow_returnsTrue_whenCooldownIsZero() {
        // A zero-length cooldown means "any elapsed time is enough" - deterministic
        // way to assert the window has passed without sleeping in a test.
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.allow("key", Duration.ZERO)).isTrue();
        assertThat(limiter.allow("key", Duration.ZERO)).isTrue();
    }

    @Test
    void allow_treatsDifferentKeysIndependently() {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.allow("key-a", Duration.ofMinutes(10))).isTrue();
        assertThat(limiter.allow("key-b", Duration.ofMinutes(10))).isTrue();
        // key-a is still within its own cooldown; key-b being allowed didn't affect it.
        assertThat(limiter.allow("key-a", Duration.ofMinutes(10))).isFalse();
    }
}
