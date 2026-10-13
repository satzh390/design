package com.example.ratelimiter.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.ratelimiter.RateLimiterPolicyStore;
import com.example.ratelimiter.model.Algorithm;
import com.example.ratelimiter.model.FailMode;
import com.example.ratelimiter.model.RateLimitResult;
import com.example.ratelimiter.model.RateLimiterPolicy;
import com.example.ratelimiter.model.TimeUnit;

class RateLimiterExecutorTest {

    @Test
    void executeReturnsAllowedWhenPolicyDoesNotExist() {
        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(), new RateLimiterPolicyStore(List.of()));

        RateLimitResult result = executor.execute("missing-policy", "user-123");

        assertTrue(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(0, result.retryAfterMs());
    }

    @Test
    void executeDelegatesToConfiguredRateLimiter() {
        RateLimiterPolicy policy = policy("token-policy", Algorithm.TOKEN_BUCKET, FailMode.FAIL_OPEN);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.getAlgorithm()).thenReturn(Algorithm.TOKEN_BUCKET);
        when(limiter.isAllowed(eq(policy), eq("user-123"))).thenReturn(new RateLimitResult(true, 15.0, 0L));

        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(limiter), new RateLimiterPolicyStore(List.of(policy)));

        RateLimitResult result = executor.execute("token-policy", "user-123");

        assertTrue(result.allowed());
        assertEquals(15.0, result.remainingTokens());
        assertEquals(0L, result.retryAfterMs());
        verify(limiter).isAllowed(policy, "user-123");
    }

    @Test
    void executeFailsOpenWhenAlgorithmIsMissing() {
        RateLimiterPolicy policy = policy("token-policy", Algorithm.TOKEN_BUCKET, FailMode.FAIL_OPEN);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.getAlgorithm()).thenReturn(Algorithm.FIXED_WINDOW);

        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(limiter), new RateLimiterPolicyStore(List.of(policy)));

        RateLimitResult result = executor.execute("token-policy", "user-123");

        assertTrue(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(0, result.retryAfterMs());
    }

    @Test
    void executeFailsClosedWhenAlgorithmIsMissing() {
        RateLimiterPolicy policy = policy("token-policy", Algorithm.TOKEN_BUCKET, FailMode.FAIL_CLOSED);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.getAlgorithm()).thenReturn(Algorithm.FIXED_WINDOW);

        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(limiter), new RateLimiterPolicyStore(List.of(policy)));

        RateLimitResult result = executor.execute("token-policy", "user-123");

        assertFalse(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(0, result.retryAfterMs());
    }

    @Test
    void executeFailsOpenWhenLimiterThrowsException() {
        RateLimiterPolicy policy = policy("token-policy", Algorithm.TOKEN_BUCKET, FailMode.FAIL_OPEN);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.getAlgorithm()).thenReturn(Algorithm.TOKEN_BUCKET);
        when(limiter.isAllowed(any(), any())).thenThrow(new RuntimeException("redis unavailable"));

        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(limiter), new RateLimiterPolicyStore(List.of(policy)));

        RateLimitResult result = executor.execute("token-policy", "user-123");

        assertTrue(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(0, result.retryAfterMs());
    }

    @Test
    void executeFailsClosedWhenLimiterThrowsException() {
        RateLimiterPolicy policy = policy("token-policy", Algorithm.TOKEN_BUCKET, FailMode.FAIL_CLOSED);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.getAlgorithm()).thenReturn(Algorithm.TOKEN_BUCKET);
        when(limiter.isAllowed(any(), any())).thenThrow(new RuntimeException("redis unavailable"));

        RateLimiterExecutor executor = new RateLimiterExecutor(List.of(limiter), new RateLimiterPolicyStore(List.of(policy)));

        RateLimitResult result = executor.execute("token-policy", "user-123");

        assertFalse(result.allowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(0, result.retryAfterMs());
    }

    private RateLimiterPolicy policy(String key, Algorithm algorithm, FailMode failMode) {
        RateLimiterPolicy policy = new RateLimiterPolicy();
        policy.setKey(key);
        policy.setAlgo(algorithm);
        policy.setFailMode(failMode);
        policy.setLimit(10L);
        policy.setTimeUnit(TimeUnit.MINUTE);
        return policy;
    }
}
