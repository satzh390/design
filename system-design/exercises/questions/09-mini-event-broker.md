# 09 — Mini Event Broker

## Problem

Build a simplified Kafka-like event broker to understand the fundamental mechanics of partitioned logs.

This is a learning implementation, not a Kafka replacement.

## Functional Requirements

1. Create a topic.
2. Create partitions.
3. Publish messages.
4. Consume messages.
5. Track consumer offsets.
6. Support consumer groups.
7. Preserve ordering within a partition.
8. Replay messages from an earlier offset.

## Architecture

```text
Producer
   |
   v
Broker
 +---------+---------+
 | Partition 0       |
 | Partition 1       |
 | Partition 2       |
 +---------+---------+
           |
           v
     Consumer Group
      /     |           C1     C2      C3
```

## NFRs

- Ordered append per partition.
- Durable messages.
- Consumers can resume.
- Slow consumers should not block unrelated partitions.
- Message loss should be measurable/testable.

## Key Concepts

- partitioning
- offsets
- consumer groups
- ordering
- log structure
- retention
- backpressure
- replication
- producer acknowledgements

## Milestones

V1: single-node append-only log.

V2: partitions.

V3: consumer groups.

V4: persistence/replay.

V5: simple replication.

## Scope Limit

Do not implement Kafka's full protocol.

Do not implement transactions, exactly-once semantics, rack awareness or production-grade replication.

## Interview Value

This project gives you a concrete understanding of why Kafka works the way it does instead of treating Kafka as a black box.
