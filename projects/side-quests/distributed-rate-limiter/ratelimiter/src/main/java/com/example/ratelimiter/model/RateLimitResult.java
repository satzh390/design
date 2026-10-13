package com.example.ratelimiter.model;

public record RateLimitResult(
        boolean allowed,
        double remainingTokens,
        long retryAfterMs) {
}