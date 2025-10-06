# Conflict Resolution Algorithms in Distributed Systems

This repository contains **algorithms and techniques to resolve conflicts** that arise in distributed systems due to **replication and concurrent writes**.  
Each algorithm includes a **description, key characteristics, example implementation, and references**.

---

## Table of Contents

- [Overview](#overview)
- [Conflict Resolution Algorithms](#conflict-resolution-algorithms)
- [References](#references)

---

## Overview

Replication improves **availability** but introduces **inconsistencies** among replicas when updates occur concurrently.  

Conflict resolution algorithms are used to:

- Detect conflicting versions  
- Reconcile conflicts in a deterministic or application-specific manner  
- Ensure **eventual consistency** across replicas  

---

## Conflict Resolution Algorithms

| Algorithm | Description | Key Characteristics | Example Implementation | References |
|-----------|-------------|-------------------|----------------------|------------|
| **Vector Clocks** | Uses a `[server: version]` pair per data item to track causality and detect conflicts. | - Detects concurrent updates<br>- Supports branching and merging<br>- Eventual consistency | [Java Example](./VectorClock.java) | [Dynamo Paper](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf), [Wikipedia](https://en.wikipedia.org/wiki/Vector_clock) |
| **Last-Writer-Wins (LWW)** | Resolves conflicts by keeping the version with the **latest timestamp**. | - Simple to implement<br>- Can overwrite updates<br>- Minimal metadata | TBD | TBD |
| **CRDT (Conflict-Free Replicated Data Type)** | Data structures that **automatically merge concurrent updates** without conflicts. | - No central coordination<br>- Strong eventual consistency<br>- Handles complex data types | TBD | TBD |
| **Operational Transformation (OT)** | Used in collaborative editing systems to **merge concurrent operations** while preserving intent. | - Maintains causality<br>- Suitable for real-time collaboration<br>- Complex to implement | TBD | TBD |
