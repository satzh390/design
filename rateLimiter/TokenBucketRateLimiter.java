package rateLimiter;

import java.time.Instant;
import java.time.Duration;

public class TokenBucketRateLimiter {
    private double tokens;           // fractional tokens for precision
    private Instant lastRefill;      // last refill timestamp
    private final int capacity;      // max tokens in bucket
    private final double refillRate; // tokens per second

    public TokenBucketRateLimiter(int bucketCapacity, int refillRatePerSecond){
        this.capacity = bucketCapacity;
        this.refillRate = refillRatePerSecond;
        this.tokens = bucketCapacity;
        this.lastRefill = Instant.now();
    }

    public synchronized boolean allowRequest() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        long millis = Duration.between(lastRefill, now).toMillis();
        if (millis <= 0) return;

        double newTokens = (millis / 1000.0) * refillRate; // nano second precision
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefill = now;
    }

    // Optional: get current available tokens
    public synchronized double getTokens() {
        refill();
        return tokens;
    }
}
/*
Pros:
The algorithm is easy to implement.
Memory efficient.
Token bucket allows a burst of traffic for short periods. A request can go through as long as there are tokens left.

Cons:
Two parameters in the algorithm are bucket size and token refill rate. However, it might be challenging to tune them properly.
 */