# 06 — Distributed Lock Service

## Problem

Multiple application instances sometimes need exclusive access to a logical resource.

Example:

```text
Only one worker should process:
daily-report-2026-09-27
```

Build a small lock abstraction.

## Functional Requirements

1. Acquire lock.
2. Release lock.
3. Lease expiration.
4. Lock ownership.
5. Renewal.
6. Prevent stale owners from modifying protected resources.

## Architecture

```text
Worker A ----Worker B -----+--> Lock Store
Worker C ----/
```

Use Redis initially.

## NFRs

- No permanent deadlocks.
- Safe under worker crashes.
- Clear ownership.
- Lock acquisition should be atomic.
- Expired locks should eventually become available.

## Critical Concept

A lease alone is not enough.

Consider:

```text
Worker A gets lock
Worker A pauses for 30 seconds
Lease expires
Worker B gets lock
Worker A wakes up
Worker A writes to resource
```

Introduce a **fencing token**:

```text
A -> token 41
B -> token 42
```

The protected resource rejects token 41 after token 42 exists.

## Key Concepts

- distributed locks
- leases
- fencing tokens
- clock/timeout issues
- split brain
- failure recovery
- Redis atomic operations

## Scope Limit

Do not claim this is a general-purpose replacement for ZooKeeper/etcd.
The goal is to understand why distributed locking is difficult.
