# Rate Limiter Designs & Implementations

This folder collects designs, models, and code for **rate limiting** approaches and algorithms — how to throttle requests, enforce quotas, and provide fair resource access.

## 📂 Folder Contents

| File / Component | Purpose |
|------------------|---------|
| `RateLimiter*.java` | Java classes implementing different rate limiting strategies (token bucket, leaky bucket, sliding window, fixed window, or custom) |
| (Optional) Interface definitions | Common rate limiter interface / abstraction for plugging different strategies |
| Tests / demo | Sample usage of the rate limiter in simulated request loads, correctness checks, concurrency handling |

## 🎯 Motivation & Design Goals

- Prevent abuse of APIs or resources by limiting request rate per user / IP / key  
- Support **burst capacity** while maintaining long-term average rate  
- Be **efficient**, **thread-safe**, and **low-overhead**  
- Modular and interchangeable strategies: one can swap token bucket with sliding window easily  
- Clean code following SOLID principles, good abstraction boundaries

## 🧩 Rate Limiting Strategies (Overview)

Here are common rate limiting strategies you may find here:

| Strategy | Behavior | Pros vs Cons |
|----------|----------|----------------|
| Fixed Window | Count requests per fixed time window (e.g. per minute) | Simple but suffers “boundary bursts” |
| Sliding Window | Maintain sliding window of time, more precise | More storage / computation |
| Token Bucket | Allow burst up to bucket capacity, refill at steady rate | Flexible and widely used |
| Leaky Bucket | Treat requests as queue, leak at constant rate | Smooth rate, queueing nature |
| Hybrid / Custom | Combined logic for burst + fairness, distributed quotas, etc. | Tailored for special constraints |

## 🔄 Usage / How to Use

1. Instantiate a rate limiter strategy with parameters (e.g. `maxRequests`, `windowSizeMs`, `bucketCapacity`).  
2. On each request, call something like `boolean tryAcquire(key)` (or `acquire(key)`) to check/consume quota.  
3. Handle concurrency (synchronize, atomic counters, locks) if used in multi-threaded contexts.  
4. Optionally, reset or refresh state (for fixed-window) or cleanup old entries (for sliding window).

```java
RateLimiter limiter = new TokenBucketRateLimiter(100 /* per min */, 200 /* burst cap */);
if (limiter.tryAcquire(userId)) {
    // allowed
} else {
    // rate limit exceeded, reject or wait
}
