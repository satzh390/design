package RateLimiter;

public class SlidingWindowCounterRateLimiter {
    private final int windowSizeInMillis;
    private final int numberOfBuckets;
    private final int maxRequests;
    private final int[] buckets;
    private long lastBucketTime;

    public SlidingWindowCounterRateLimiter(int maxRequests, int windowSizeInMillis, int numberOfBuckets) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.numberOfBuckets = numberOfBuckets;
        this.buckets = new int[numberOfBuckets];
        this.lastBucketTime = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        int bucketSize = windowSizeInMillis / numberOfBuckets;
        int currentBucket = (int)((now / bucketSize) % numberOfBuckets);

        // Reset stale bucket if time moved
        long elapsedBuckets = (now - lastBucketTime) / bucketSize;
        for (int i = 1; i <= Math.min(elapsedBuckets, numberOfBuckets); i++) {
            int idx = (currentBucket + i) % numberOfBuckets;
            buckets[idx] = 0;
        }

        lastBucketTime = now;
        buckets[currentBucket]++;

        // Sum all buckets
        int total = 0;
        for (int count : buckets) total += count;

        return total <= maxRequests;
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowCounterRateLimiter limiter = new SlidingWindowCounterRateLimiter(10, 10000, 5); // 10 req / 10s, 5 buckets

        for (int i = 0; i < 20; i++) {
            System.out.println(limiter.allowRequest());
            Thread.sleep(1000);
        }
    }
}

/*
Pros
It smooths out spikes in traffic because the rate is based on the average rate of the previous window.
Memory efficient.

Cons
It only works for not-so-strict look back window. It is an approximation of the actual rate because it assumes requests in the previous window are evenly distributed. However, this problem may not be as bad as it seems. According to experiments done by Cloudflare [10], only 0.003% of requests are wrongly allowed or rate limited among 400 million requests.
 */