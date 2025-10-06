# Distributed Systems & Consensus Algorithms

This repository explores different **consensus algorithms** used in distributed systems, focusing on **data replication, synchronization, and consistency**. Data replicated across multiple nodes must be kept consistent, and consensus algorithms are key to achieving this.

---

## Table of Contents

- [Introduction](#introduction)
- [Quorum Consensus](#quorum-consensus)
  - [Definitions](#definitions)
  - [How It Works](#how-it-works)
  - [Configuration Examples](#configuration-examples)
  - [References](#references)
- [Other Consensus Algorithms](#other-consensus-algorithms)
  - [Paxos](#paxos)
  - [Raft](#raft)
  - [Gossip Protocol](#gossip-protocol)
- [GitHub Repository Reference](#github-repository-reference)

---

## Introduction

In distributed systems, data is often replicated across multiple nodes to improve fault tolerance and availability. Ensuring **consistency** among replicas is challenging, and different consensus algorithms are used to solve this problem.

Consensus algorithms allow distributed nodes to agree on a single value (e.g., a data write) even in the presence of failures.

---

## Quorum Consensus

**Quorum consensus** is a widely used method to guarantee **consistency for read and write operations**.

### Definitions

- **N**: Total number of replicas in the system.
- **W**: Write quorum size. A write is successful if at least W replicas acknowledge it.
- **R**: Read quorum size. A read is successful if responses are received from at least R replicas.

> **Strong consistency condition:** `W + R > N`

### How It Works

1. A client sends a **write request** (e.g., `put(key, value)`) to a **coordinator** node.
2. The coordinator forwards the request to all replicas.
3. Each replica stores the data and responds with an **ACK**.
4. Once the coordinator receives **W acknowledgments**, the write is considered successful.
5. Similarly, a **read request** is sent to the replicas, and once **R responses** are received, the read is successful.

**Example:**  

- N = 3 (s0, s1, s2)  
- W = 1 (coordinator waits for at least 1 ACK)  
- R = 2 (read waits for 2 responses)  

Here, even if one replica is slow, write operations can succeed quickly, and reads are consistent as long as `W + R > N`.

#### Notes

- `W = 1` does **not** mean data is written on only one node. Data is replicated to all nodes; it only means the coordinator can consider the write complete after **one acknowledgment**.
- Choosing `W`, `R`, and `N` is a **tradeoff between latency and consistency**.

### Configuration Examples

| N | W | R | Characteristics                       |
|---|---|---|--------------------------------------|
| 3 | 3 | 1 | Fast reads, slower writes             |
| 3 | 1 | 3 | Fast writes, slower reads             |
| 3 | 2 | 2 | Strong consistency (overlapping node)|
| 3 | 1 | 1 | Fastest operations, eventual consistency|

> Tip: Adjust `W`, `R`, `N` according to your system requirements.

### References

- [Quorum Consensus – Wikipedia](https://en.wikipedia.org/wiki/Quorum-based_replication)
- [Cassandra Consistency Levels](https://cassandra.apache.org/doc/stable/cassandra/architecture/dynamo.html#tunable-consistency)

---

## Other Consensus Algorithms

### Paxos

Paxos is a family of protocols for achieving consensus in a network of unreliable processors. It is **widely used but complex to implement**.

- Guarantees **safety** (no two nodes decide differently) and **liveness** (eventually a value is chosen if nodes continue participating).  
- Common references:
  - [Paxos Made Simple](https://lamport.azurewebsites.net/pubs/paxos-simple.pdf)

### Raft

Raft is an alternative to Paxos that is **simpler to understand and implement**. It is used in systems like **etcd** and **Consul**.

- Leader election for log replication
- Ensures consistency through **majority agreement**
- Reference: [The Raft Consensus Algorithm](https://raft.github.io/)

---



