# 05 — Distributed Cache Service

## Problem

Build a small cache layer that reduces database reads while remaining understandable under concurrency and failure.

## Functional Requirements

1. GET
2. SET
3. DELETE
4. TTL
5. Cache statistics
6. Cache-aside client example
7. Configurable maximum size

## Architecture

```text
Application
   |
   v
Cache Client
   |
   v
Redis
   |
   v
Database
```

Then experiment with:

```text
Application
   |
 Local Cache
   |
 Redis
   |
 Database
```

## NFRs

- Fast reads.
- Bounded memory.
- Expiration support.
- Graceful cache failure.
- Prevent unnecessary database load.

## Key Concepts

- cache-aside
- write-through
- write-back
- TTL
- eviction
- cache invalidation
- cache stampede
- thundering herd
- hot keys
- stale data
- negative caching

## Experiments

Compare:

1. No cache.
2. Redis cache.
3. Local + Redis cache.

Measure latency and database request reduction.

## Scope Limit

Do not build Redis.

The learning target is cache architecture and trade-offs.
