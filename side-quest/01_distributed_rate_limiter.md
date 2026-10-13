# 01 — Distributed Rate Limiter

## Problem

Provide a service that decides whether a request should be allowed under a configurable rate limit.

Example:

`user:123 -> 100 requests/minute`

The service must work correctly when multiple application instances call it concurrently.

## Why this project

Teaches one of the cleanest distributed-systems problems without requiring a huge application.

## Functional Requirements

1. Check whether a request is allowed.
2. Support a key such as user ID, API key or IP.
3. Support configurable limits.
4. Return remaining quota.
5. Support at least two algorithms:
   - Fixed Window
   - Token Bucket
6. Support concurrent callers.
7. Expose REST API.


## Non-Functional Requirements

- Low latency: target p95 < 10 ms for the limiter itself in local deployment.
- Atomic decision.
- Horizontal application scaling.
- No incorrect double allowance under concurrency.
- Graceful behavior when Redis is unavailable.
- Observable decision latency and rejection rate.

## Proposed Architecture

```text
Clients
   |
   v
Rate Limit API
   |
   v
Redis
   |
   +-- atomic Lua operation
   +-- counters / token state
```

## Key Concepts

- Token bucket
- Fixed/sliding windows
- Redis atomicity
- Lua scripts
- race conditions
- distributed counters
- fail-open vs fail-closed
- clock issues
- hot keys

## Scope Limit

Do not build a full API gateway.

No authentication, billing, analytics platform or multi-region support.

## Stretch Goal

Benchmark:

- 1 node
- 3 nodes
- high contention on one key

Compare local-only and Redis-backed approaches.

## Interview Questions

- Why Redis?
- Why Lua?
- What happens if Redis fails?
- What happens with 100 application nodes?
- Can you guarantee exact limits?
- How would you handle multi-region traffic?
