package RateLimiter;

import java.time.Duration;
import java.time.Instant;

public class TokenBucketRateLimiter {
    private int tokens;
    private Instant refilledAt;
    private final int refillRate;
    private final int capacity;

    public TokenBucketRateLimiter(int bucketCapacity, int refillRatePerSecond){
        this.capacity = bucketCapacity;
        this.refillRate = refillRatePerSecond;
        this.tokens = bucketCapacity;
        this.refilledAt = Instant.now();
    }

    public synchronized boolean allowRequest(){
        refill();
        if(tokens > 0) {
            tokens -= 1;
            return true;
        }

        return false;
    }
    
    private void refill(){
        Instant now = Instant.now();
        Duration elapsed = Duration.between(refilledAt, now);
        if(elapsed.getSeconds() > 0){
            long currentTokens = tokens + elapsed.getSeconds() * refillRate;
            tokens = (int)Math.min(currentTokens, capacity); 
            refilledAt = now;
        }
    }
}