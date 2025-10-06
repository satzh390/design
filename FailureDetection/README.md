# Failure Detection Algorithms in Distributed Systems

This repository explores **different failure detection algorithms** used in distributed systems to maintain **system health, reliability, and consistency**. Each algorithm includes a brief description, characteristics, implementation, and references.

---

## Table of Contents

- [Overview](#overview)
- [Failure Detection Algorithms](#failure-detection-algorithms)
- [References](#references)

---

## Overview

In distributed systems, nodes can fail unpredictably due to crashes, network issues, or hardware faults. Failure detection algorithms help the system **identify failed nodes** and take corrective actions such as re-routing, replication, or rebalancing workloads.  

Key requirements of failure detection algorithms:  
- **Accuracy:** Correctly detect failed nodes.  
- **Timeliness:** Detect failures quickly to avoid cascading issues.  
- **Scalability:** Work efficiently in large clusters.  

---

## Failure Detection Algorithms

| Algorithm | Description | Key Characteristics | Example Implementation | References |
|-----------|-------------|-------------------|----------------------|------------|
| **Gossip Protocol** | Nodes periodically exchange state with random peers to propagate information. Eventually, all nodes converge to the same state. | - Decentralized<br>- Eventual consistency<br>- Scalable and fault-tolerant | [Java Implementation](./GossipProtocol.java) | [Cassandra](https://cassandra.apache.org/doc/stable/cassandra/architecture/dynamo.html#gossip) |
| **Heartbeat Protocol** | Nodes send periodic "heartbeat" messages to neighbors or a monitoring service. If a heartbeat is missed, the node is suspected to have failed. | - Simple<br>- Timely detection<br>- Can produce false positives under network delays | TBD | TBD |
| **Phi Accrual Failure Detector** | Probabilistic failure detector that computes a suspicion level (phi) based on heartbeat arrival patterns. | - Adaptive to network delays<br>- Less false positives<br>- Widely used in Cassandra | TBD | TBD |
| **SWIM (Scalable Weakly-consistent Infection-style Process Group Membership)** | Combines gossip and failure detection. Nodes periodically ping a random node, gossiping results of membership checks. | - Low overhead<br>- Fast detection<br>- Strong scalability | TBD | TBD |
| **Centralized Monitoring** | A central node monitors all nodes’ health and reports failures. | - Simple to implement<br>- Single point of failure<br>- Not scalable | TBD | TBD |
