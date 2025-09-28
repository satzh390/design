package RateLimiter;

import java.time.Duration;
import java.time.Instant;

public class LeakyBucketRateLimiter {
    private double water;            // fractional water for precision
    private Instant lastChecked;     // last leak timestamp
    private final int capacity;      // max bucket size
    private final double leakRate;   // tokens leaking per second

    public LeakyBucketRateLimiter(int capacity, int leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRate = leakRatePerSecond;
        this.water = 0;
        this.lastChecked = Instant.now();
    }

    public synchronized boolean allowRequest() {
        leak();
        if (water < capacity) {
            water += 1; // add incoming request
            return true;
        }
        return false; // bucket full
    }

    private void leak() {
        Instant now = Instant.now();
        long millis = Duration.between(lastChecked, now).toMillis();
        if (millis <= 0) return;

        double leaked = (millis / 1000.0) * leakRate;
        water = Math.max(0, water - leaked);
        lastChecked = now;
    }

    // Optional: get current water level
    public synchronized double getWaterLevel() {
        leak();
        return water;
    }
}

/*
Pros:
Memory efficient given the limited queue size.
Requests are processed at a fixed rate therefore it is suitable for use cases that a stable outflow rate is needed.

Cons:
A burst of traffic fills up the queue with old requests, and if they are not processed in time, recent requests will be rate limited.
There are two parameters in the algorithm. It might not be easy to tune them properly.
 */