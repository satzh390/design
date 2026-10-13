# 10 — Distributed Key-Value Store

## Problem

Build a deliberately small distributed key-value database to understand partitioning, replication and consistency.

API:

```text
PUT /kv/user:123
GET /kv/user:123
DELETE /kv/user:123
```

## Functional Requirements

1. Store key/value.
2. Retrieve key/value.
3. Delete key/value.
4. Partition keys across nodes.
5. Replicate data.
6. Recover a node.
7. Define read/write consistency behavior.
8. Expose node health.

## Architecture

```text
                 Client
                   |
                   v
             Routing Layer
                   |
        +----------+----------+
        |          |          |
      Node A     Node B     Node C
       /  \       /  \       /  \
     R1   R2     R2   R3     R3   R1
```

## NFRs

- Horizontal scalability.
- No single storage node should contain all data.
- Node failure should not make all data unavailable.
- Recovery should eventually restore replicas.
- Latency and consistency behavior should be measurable.

## Key Concepts

- consistent hashing
- partitioning
- replication
- quorum
- leader/follower
- read repair
- hinted handoff
- eventual consistency
- failure detection
- rebalancing

## Scope Limit

This is the final and largest side quest.

Do not attempt a production database.

Use small values, a small number of nodes and a simplified protocol.

## Suggested Implementation

Start with 3 JVM processes on one machine.

Then simulate:

- node crash
- network delay
- stale replica
- partition movement

## Interview Goal

After completing this project, you should be able to explain:

- How data is partitioned.
- How replicas are selected.
- What happens during node failure.
- What consistency guarantees mean.
- Why CAP trade-offs appear.
- How rebalancing works.
