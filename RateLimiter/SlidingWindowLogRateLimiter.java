package RateLimiter;

import java.util.LinkedList;
import java.util.Queue;

public class SlidingWindowLogRateLimiter {
    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Queue<Long> requestTimestamps;

    public SlidingWindowLogRateLimiter(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestTimestamps = new LinkedList<>();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        // Remove timestamps older than window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() <= now - windowSizeInMillis) {
            requestTimestamps.poll();
        }

        if (requestTimestamps.size() < maxRequests) {
            requestTimestamps.add(now);
            return true;
        } else {
            return false; // limit reached
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowLogRateLimiter limiter = new SlidingWindowLogRateLimiter(5, 10000); // 5 requests / 10 seconds

        for (int i = 0; i < 10; i++) {
            System.out.println(limiter.allowRequest());
            Thread.sleep(1500);
        }
    }
}

/*
Pros:
Rate limiting implemented by this algorithm is very accurate. In any rolling window, requests will not exceed the rate limit.

Cons:
The algorithm consumes a lot of memory because even if a request is rejected, its timestamp might still be stored in memory.
 */
