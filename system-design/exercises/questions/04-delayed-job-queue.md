# 04 — Delayed Job / Task Queue

## Problem

Build a small distributed task queue where jobs can execute immediately or at a future time.

Example:

```text
send-email       now
cleanup          +1 hour
retry-payment    +10 minutes
```

## Functional Requirements

1. Submit a job.
2. Schedule a future execution time.
3. Workers claim jobs.
4. Prevent two workers from processing the same job simultaneously.
5. Retry failed jobs.
6. Timeout abandoned jobs.
7. Expose job status.
8. Support cancellation.

## Architecture

```text
Producer
   |
   v
Job API
   |
   v
PostgreSQL / Queue
   |
   v
Scheduler
   |
   v
Workers
```

Possible later version:

```text
Kafka + scheduler + worker pool
```

## NFRs

- Jobs must not disappear after process restart.
- A worker crash should make the job available again.
- Duplicate execution must be handled.
- Scheduler should not scan the entire table every second.
- Job execution latency should be measurable.

## Key Concepts

- visibility timeout
- leases
- worker heartbeats
- delayed queues
- retries
- scheduling
- race conditions
- polling vs event-driven scheduling

## Important Design Question

How do you prevent:

```text
Worker A claims job
Worker A becomes slow
Lease expires
Worker B claims job
Worker A continues
```

This introduces fencing/lease concepts that are useful in interviews.

## Scope Limit

Do not build Airflow.

No DAGs in this project.
