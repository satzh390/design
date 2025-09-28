package RateLimiter;

public class FixedWindowRateLimiter {
    private final int maxRequests;
    private final long windowSizeInMillis;
    private int requestCount;
    private long windowStart;

    public FixedWindowRateLimiter(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.requestCount = 0;
        this.windowStart = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        if (now - windowStart >= windowSizeInMillis) {
            // reset the window
            windowStart = now;
            requestCount = 0;
        }

        if (requestCount < maxRequests) {
            requestCount++;
            return true;
        } else {
            return false; // limit reached
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(5, 10000); // 5 requests / 10 seconds

        for (int i = 0; i < 10; i++) {
            System.out.println(limiter.allowRequest());
            Thread.sleep(1500); // simulate request every 1.5 sec
        }
    }

}
/*
Pros:
Memory efficient.
Easy to understand.
Resetting available quota at the end of a unit time window fits certain use cases.

Cons:
Spike in traffic at the edges of a window could cause more requests than the allowed quota to go through.
 */