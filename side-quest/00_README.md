# System Design Side Quests — 10 Practical Projects + Payment Gateway

## Purpose

This repository is a personal learning portfolio, not a production SaaS portfolio.

The goal is to build a set of small, deliberately scoped systems that teach the core distributed-systems ideas commonly discussed in backend/system-design interviews:

- API design
- concurrency
- idempotency
- caching
- rate limiting
- queues
- retries and backoff
- scheduling
- partitioning
- ordering
- delivery semantics
- fault tolerance
- consistency
- replication
- observability
- backpressure
- distributed coordination
- eventual consistency
- schema evolution

Each project should be small enough for one person to complete incrementally at roughly **3–6 hours/week**, with a hard upper bound of about **6–7 hours/week**.

## Important rule

Do NOT try to productionize these systems.

The objective is:

> Build a small working system, understand its trade-offs, test its failure modes, document the architecture, and be able to explain why it was designed that way.

A project is complete when you can answer:

1. What problem does it solve?
2. What are the functional requirements?
3. What are the NFRs?
4. What happens when a component fails?
5. What consistency model is being used?
6. Where can duplicates happen?
7. Where can data be lost?
8. How does the system scale?
9. What is the bottleneck?
10. What would change at 10x and 100x scale?

## Suggested stack

- Java 21+
- Spring Boot
- PostgreSQL
- Redis
- Kafka
- Docker / Docker Compose
- Angular only where a UI genuinely helps
- JUnit + Testcontainers
- OpenTelemetry / Prometheus where useful

Do not force every technology into every project.

## Ten projects

| # | Project | Main concepts | Target |
|---|---|---|---|
| 1 | Distributed Rate Limiter | Redis, atomicity, algorithms, concurrency | 1–2 weeks |
| 2 | Idempotency & Request Deduplication Service | exactly-once illusion, retries, DB constraints | 1–2 weeks |
| 3 | Reliable Webhook Delivery Engine | queues, retries, DLQ, backoff, delivery semantics | 2–3 weeks |
| 4 | Delayed Job / Task Queue | scheduling, visibility timeout, workers, leases | 2–3 weeks |
| 5 | Distributed Cache Service | cache-aside, TTL, invalidation, stampede | 2–3 weeks |
| 6 | Distributed Lock Service | leases, fencing tokens, failure scenarios | 1–2 weeks |
| 7 | Mini Workflow / DAG Engine | dependency graphs, workers, state machines | 3–4 weeks |
| 8 | PostgreSQL CDC Pipeline | WAL, offsets, checkpoints, ordering, sinks | 3–4 weeks |
| 9 | Mini Event Broker | partitions, consumer groups, offsets, ordering | 3–4 weeks |
| 10 | Distributed Key-Value Store | partitioning, replication, quorum, consistency | 4–6 weeks |\n| 13 | Payment Gateway / Payment Orchestration Service | payments, idempotency, webhooks, state machines, reconciliation | 2–3 weeks |

These are intentionally ordered from smaller building blocks toward larger distributed systems.

## Weekly operating model

Normal week:

- 2 × 45-minute weekday sessions
- 1 × 3–4 hour weekend session
- Total: ~4.5–5.5 hours

Busy interview week:

- 2–3 hours only
- Read/design/test one small piece

Heavy work week:

- Skip the project completely

The project must never become another job.

## Portfolio evidence

Every project should contain:

- requirements.md
- architecture.md
- ADRs for important decisions
- README with scope and limitations
- load/failure test notes
- one architecture diagram
- one "what I learned" section

The strongest interview statement is not:

> "I built ten production systems."

It is:

> "I built a series of deliberately scoped distributed systems to understand the trade-offs behind rate limiting, idempotency, queues, retries, scheduling, consistency, partitioning and replication. I benchmarked and tested their failure modes and documented the design decisions."

## Definition of Done

For each project:

- [ ] Working MVP
- [ ] Unit tests
- [ ] At least one integration test
- [ ] At least one failure-mode test
- [ ] Architecture diagram
- [ ] README
- [ ] Trade-off notes
- [ ] One short system-design interview explanation
