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

---

## Handling temporary failures

After failures have been detected through the gossip protocol, the system needs to deploy certain mechanisms to ensure availability. In the strict quorum approach, read and write operations could be blocked as illustrated in the quorum consensus section.

A technique called “sloppy quorum” [4] is used to improve availability. Instead of enforcing the quorum requirement, the system chooses the first W healthy servers for writes and first R healthy servers for reads on the hash ring. Offline servers are ignored.

If a server is unavailable due to network or server failures, another server will process requests temporarily. When the down server is up, changes will be pushed back to achieve data consistency. This process is called hinted handoff. Since s2 is unavailable in Figure 12, reads and writes will be handled by s3 temporarily. When s2 comes back online, s3 will hand the data back to s2.

---

## Handling permanent failures

Hinted handoff is used to handle temporary failures. What if a replica is permanently unavailable? To handle such a situation, we implement an anti-entropy protocol to keep replicas in sync. Anti-entropy involves comparing each piece of data on replicas and updating each replica to the newest version. A Merkle tree is used for inconsistency detection and minimizing the amount of data transferred.

Quoted from Wikipedia [7]: “A hash tree or Merkle tree is a tree in which every non-leaf node is labeled with the hash of the labels or values (in case of leaves) of its child nodes. Hash trees allow efficient and secure verification of the contents of large data structures”.
