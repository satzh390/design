# 08 — PostgreSQL Change Data Capture Pipeline

## Problem

Capture database changes and publish them as events.

Start with PostgreSQL only.

## Functional Requirements

1. Detect INSERT/UPDATE/DELETE changes.
2. Convert changes into a stable event format.
3. Maintain a durable offset/checkpoint.
4. Resume after restart.
5. Deliver to at least two sinks.
6. Preserve ordering for a chosen scope.

Possible sinks:

- Kafka
- HTTP
- file/stdout

## Architecture

```text
PostgreSQL
    |
    | WAL / logical replication
    v
CDC Reader
    |
    v
Event Normalizer
    |
    v
Checkpoint Store
    |
    +---- Kafka
    +---- HTTP
```

## NFRs

- No silent event loss.
- Restartable from checkpoint.
- Duplicate events must be possible and documented.
- Bounded memory.
- Backpressure when sinks are slow.
- Observable lag.

## Key Concepts

- WAL
- logical replication
- offsets
- checkpoints
- ordering
- at-least-once delivery
- backpressure
- schema evolution
- replay
- consumer lag

## Important Design Question

What happens if:

```text
Read WAL event
   |
Send to Kafka succeeds
   |
Process crashes before checkpoint
```

After restart, the event may be sent again.

That is not necessarily a bug; it is a delivery-semantics decision.

## Scope Limit

Do not attempt to compete with Debezium.

Support one database and a few sinks.

The objective is understanding CDC internals and distributed delivery.
