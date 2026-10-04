# DDIA Recall Sheet — Up to Replication

> **Source:** *Designing Data-Intensive Applications* by Martin Kleppmann  
> **Coverage:** Chapters 1–5, through Replication (roughly the first ~200 pages of the referenced edition/PDF).  
> **Purpose:** Fast recall after weeks/months — not a replacement for the book. Use the book when a concept needs deeper understanding.

---

## 0. The Big Picture

A data-intensive application is primarily constrained by:

- **Data volume**
- **Data complexity**
- **Data velocity**
- **Availability / reliability**
- **Latency / throughput**
- **Operational complexity**

The recurring design questions:

1. How is data modeled?
2. How is data stored and retrieved efficiently?
3. How is data encoded and moved between processes?
4. How do multiple machines coordinate / replicate data?
5. What happens when machines, networks, or processes fail?

Think of the first five chapters as:

~~~text
Data models
    ↓
Storage & retrieval
    ↓
Encoding / evolution
    ↓
Replication
~~~

---

# 1. Reliable, Scalable and Maintainable Applications

## Reliability

**A system continues to work correctly even when faults occur.**

Fault ≠ failure:

- **Fault:** one component deviates from normal behavior.
- **Failure:** the whole service stops providing the required service.

Common faults:

- Hardware failure
- Process crash
- Network failure
- Human error
- Software bug
- Dependency failure
- Overload

### Fault-tolerance

Design so that a fault in one component does not necessarily become a system-wide failure.

Examples:

- Replication
- Automatic failover
- Checksums
- Retries
- Backups
- Idempotency
- Monitoring

---

## Scalability

Scalability is not a single number.

Ask:

> If the load grows, how does the system respond?

Important load parameters:

- Requests/sec
- Read/write ratio
- Data size
- Number of users
- Concurrent connections
- Fan-out
- Message rate

### Latency vs response time

- **Latency:** time for a request to be processed / delayed by the system.
- **Response time:** what the client observes end-to-end.

### Percentiles

Average latency hides bad tail behavior.

Use:

- p50 → median
- p95 → 95% of requests are faster
- p99 → 99% are faster
- p99.9 → tail latency

**Tail latency matters** especially when one request fans out to many downstream services.

---

## Throughput

Amount of work completed per unit time.

Examples:

- requests/sec
- MB/sec
- events/sec

Latency and throughput are different dimensions.

---

## Load parameters → performance

Always describe a system with concrete numbers.

Example:

~~~text
10M users
1M requests/sec peak
10:1 read/write ratio
500 GB/day new data
p99 < 200 ms
~~~

This makes architecture decisions meaningful.

---

## Maintainability

Three major concerns:

### Operability
Make it easy for operations teams to:

- monitor
- deploy
- debug
- recover
- roll back
- inspect health

### Simplicity
Avoid unnecessary complexity.

Good abstraction reduces accidental complexity.

### Evolvability
Requirements change.

Design so the system can evolve without rewriting everything.

---

# 2. Data Models and Query Languages

## Relational model

Data represented as:

~~~text
Tables
  ↓
Rows
  ↓
Columns
~~~

Good for:

- Structured data
- Relationships
- Joins
- Transactions
- Declarative queries

SQL describes **what** data is required, not usually **how** to find it.

---

## SQL: key ideas

### SELECT / filtering

~~~sql
SELECT ...
FROM ...
WHERE ...
~~~

### Joins

Combine related records.

Common types:

- INNER JOIN → matching rows
- LEFT JOIN → all left rows + matches
- RIGHT JOIN → all right rows + matches
- FULL OUTER JOIN → all rows from both sides

Mental model:

> A join reconstructs relationships that were separated across tables.

### Normalization

Reduce duplication and update anomalies.

Typical relational approach:

~~~text
Customer
   |
   +---- Order
          |
          +---- Product
~~~

### Denormalization

Intentionally duplicate data to optimize reads / avoid expensive joins.

Trade-off:

- Faster reads
- More storage
- More complicated updates
- Possible inconsistency

---

## Document model

Data stored as documents, commonly JSON-like:

~~~json
{
  "customer": {
    "name": "...",
    "orders": [...]
  }
}
~~~

Good when:

- Data naturally forms aggregates
- Related data is usually read together
- Schema varies
- Joins are limited

### Document DB mental model

> Store an object as an aggregate rather than reconstructing it from many tables.

But deeply nested documents can create:

- duplication
- large updates
- awkward relationships

---

## Relational vs document

### Relational

Strong at:

- joins
- many-to-many relationships
- transactions
- ad-hoc queries
- enforcing relationships

### Document

Strong at:

- aggregate-oriented data
- flexible schema
- locality of related data
- simple object-shaped reads

Neither is universally better.

---

## NoSQL

NoSQL is not one specific database model.

Common categories:

- Key-value
- Document
- Wide-column
- Graph

Typical motivations:

- Scalability
- Flexible schema
- Specialized access patterns
- High write/read throughput
- Distributed operation

---

## Wide-column / column-family model

Think:

~~~text
row key
  ↓
column family
  ↓
columns / cells
~~~

Data is often organized around the queries the application needs.

Examples/concepts:

- Cassandra
- HBase
- Bigtable-style systems

**Important:** wide-column / column-family stores are not the same thing as analytical **column-oriented storage**.

---

## Graph model

Entities → nodes  
Relationships → edges

Useful for:

- Social networks
- Recommendations
- Routing
- Knowledge graphs

Core advantage:

> Efficient traversal of relationships.

---

## Declarative vs imperative query languages

### Declarative

Say **what** you want.

~~~sql
SELECT ...
WHERE ...
~~~

Database optimizer chooses execution strategy.

### Imperative

Describe **how** to perform the operation step-by-step.

---

## MapReduce

General batch-processing model:

~~~text
Input
  ↓
Map
  ↓
Shuffle / group
  ↓
Reduce
  ↓
Output
~~~

Useful for large-scale batch processing.

---

## Schema-on-write vs schema-on-read

### Schema-on-write

Structure enforced before data is stored.

Typical relational approach.

### Schema-on-read

Interpret structure when reading.

Useful for heterogeneous / evolving data.

---

# 3. Storage and Retrieval

This chapter is about the machinery underneath databases.

The key question:

> How do we turn a logical key/value operation into efficient disk operations?

---

## Two broad families

~~~text
Storage engines
├── Log-structured
│   ├── append-only log
│   ├── hash index
│   └── LSM trees / SSTables
│
└── Page-oriented
    └── B-trees
~~~

---

# 3.1 Hash Index

Basic idea:

~~~text
key → hash → location
~~~

For an append-only key-value store:

~~~text
log:
key1=value1
key2=value2
key1=value3
~~~

Hash index points:

~~~text
key1 → latest offset
key2 → offset
~~~

### Strengths

- Fast point lookup
- Simple

### Problems

- Hash indexes don't support efficient range scans.
- Index may become too large.
- Append-only log needs cleanup.

---

# 3.2 Compaction

Suppose:

~~~text
A=1
A=2
A=3
B=4
~~~

Only latest value for A is needed.

Compaction:

~~~text
A=3
B=4
~~~

Purpose:

- Remove obsolete versions
- Reduce storage
- Keep reads efficient

---

# 3.3 SSTable

**Sorted String Table**

A file containing key/value entries sorted by key.

~~~text
A → ...
B → ...
C → ...
D → ...
~~~

Why sorted?

- Efficient range scans
- Efficient merging
- Efficient compaction
- Can use sparse indexes

---

# 3.4 LSM Tree

**Log-Structured Merge-Tree**

Core idea:

> Buffer writes in memory, flush sorted immutable files, and compact them in the background.

Simplified:

~~~text
              Writes
                ↓
          Memtable (RAM)
                ↓ flush
             SSTable
                ↓
        background compaction
                ↓
       larger sorted SSTables
~~~

Typical read path:

~~~text
Memtable
   ↓
Recent SSTables
   ↓
Older SSTables
~~~

### Why LSM is good for writes

Sequential/append-oriented writes are cheaper than random disk updates.

### Costs

- Compaction consumes I/O
- Reads may check multiple structures
- Write amplification
- Space amplification

---

## Bloom filter

Probabilistic structure answering:

> "Is this key definitely not present?"

Properties:

- **False positive:** possible
- **False negative:** no

Use it to avoid unnecessary SSTable reads.

~~~text
Bloom filter says NO → definitely absent
Bloom filter says YES → maybe present
~~~

---

## Compaction strategies

### Size-tiered style

Merge similarly sized files.

### Leveled style

Organize SSTables into levels with non-overlapping key ranges within a level.

Trade-offs involve:

- Read amplification
- Write amplification
- Space amplification

---

# 3.5 B-Tree

Most common page-oriented storage structure.

~~~text
             Root
           /      \
        Page       Page
       /  \       /  \
    Leaf  Leaf   Leaf  Leaf
~~~

Data is divided into fixed-size pages/blocks.

### Lookup

~~~text
Root
 ↓
Internal node
 ↓
Leaf
 ↓
Record
~~~

Typical complexity:

O(log_B N)

where B is the branching factor.

---

## B-tree vs LSM

| | B-tree | LSM |
|---|---|---|
| Writes | Random/page updates | Sequential/append-oriented |
| Reads | Predictable | May touch multiple SSTables |
| Range scan | Excellent | Excellent |
| Compaction | Not central | Core operation |
| Write amplification | Can be high | Can be high |
| Read amplification | Usually lower | Can be higher |

Interview mental model:

> **B-tree = update pages in place.**  
> **LSM = accumulate sorted immutable files + merge.**

---

# 3.6 Transactions

Transaction provides a way to group operations into a logical unit.

Classic ACID:

- **Atomicity** → all or nothing
- **Consistency** → preserves defined invariants
- **Isolation** → concurrent transactions don't improperly interfere
- **Durability** → committed data survives failures

Do not confuse:

> ACID consistency with distributed consistency.

They refer to different concepts.

---

# 3.7 WAL

**Write-Ahead Log**

Before modifying durable data structures:

~~~text
write log
   ↓
persist log
   ↓
modify data/page
~~~

If crash occurs, recovery replays the log.

Key idea:

> The log is the durable record of intended changes.

---

# 3.8 Secondary indexes

Primary index usually identifies the main record/key.

Secondary index supports additional lookup paths.

Example:

~~~text
Primary: user_id
Secondary: email
~~~

Trade-off:

> Every additional index makes writes more expensive.

---

# 3.9 Full-text search

Databases and search engines can build indexes for words/terms.

Inverted index:

~~~text
"java" → doc1, doc4, doc9
"redis" → doc2, doc4
~~~

Useful for text search.

---

# 3.10 Column-oriented storage

**Do not confuse with wide-column databases.**

Column-oriented analytics:

~~~text
row store:
R1: A B C
R2: A B C
R3: A B C

column store:
A: A A A
B: B B B
C: C C C
~~~

Excellent for analytical queries that read a few columns across many rows.

Benefits:

- Less data read
- Compression
- Vectorized processing
- Efficient aggregation

Typical workload:

> OLAP / analytics

---

## Row store vs column store

### Row store

Good for:

- Point lookups
- OLTP
- Updating complete records

### Column store

Good for:

- Aggregations
- Scanning millions/billions of rows
- Reading a subset of columns
- Analytics

---

# 4. Encoding and Evolution

Data does not live forever in one format.

Applications evolve:

~~~text
Version 1
   ↓
Version 2
   ↓
Version 3
~~~

Old data and new application code often coexist.

The key requirement:

> **Backward/forward compatibility.**

---

## Encoding formats

### Language-specific encoding

Easy within one language, but often poor for interoperability and long-term storage.

### JSON / XML

Human-readable and widely supported.

Trade-offs:

- Verbose
- Weak typing
- Larger payloads

### Binary formats

Examples:

- Protocol Buffers
- Avro
- Thrift

Benefits:

- Compact
- Schema-aware
- Efficient

---

## Schema evolution

Suppose old schema:

~~~text
User {
  name
}
~~~

New schema:

~~~text
User {
  name
  age
}
~~~

Old data may not contain age.

Therefore:

- New fields need sensible defaults
- Removing/renaming fields requires care
- Readers and writers must tolerate different versions

---

## Backward compatibility

New code can read old data.

## Forward compatibility

Old code can tolerate data written by newer code.

---

## REST / RPC

### REST

Resource-oriented communication, commonly over HTTP.

### RPC

Call a remote service as if invoking a local method.

But remote calls differ from local calls because of:

- network latency
- timeouts
- partial failure
- serialization
- retries
- versioning

**Mental model:**

> A remote call is not a local function call with extra syntax.

---

## Dataflow through databases

Data can flow through:

### Database
One version of application writes; another reads later.

### Services
Service A → RPC → Service B.

### Message brokers
Producer → broker → consumer.

Compatibility must survive across the entire dataflow.

---

# 5. Replication

## Why replicate?

Replication means keeping copies of the same data on multiple nodes.

Main reasons:

- **High availability**
- **Fault tolerance**
- **Read scalability**
- **Lower latency / geographic locality**

But:

> Replication creates the possibility that replicas temporarily disagree.

---

# 5.1 Three replication models

~~~text
Replication
├── Single-leader
├── Multi-leader
└── Leaderless
~~~

---

# 5.2 Single-leader replication

~~~text
             Client
                |
                v
             Leader
             /    \
            v      v
        Follower Follower
~~~

Writes go to leader.

Leader sends changes to followers.

Followers may serve reads.

### Advantages

- Simple write ordering
- Straightforward consistency model
- Common in relational databases

### Cost

- Leader can become bottleneck
- Failover is operationally complex
- Async replication creates lag

---

# 5.3 Synchronous vs asynchronous replication

### Synchronous

Leader waits for replica acknowledgement.

Pros:

- Stronger durability guarantee

Cons:

- Higher latency
- Replica failure can block progress

### Asynchronous

Leader acknowledges before replicas necessarily catch up.

Pros:

- Low write latency
- Better availability

Cons:

- Replication lag
- Potential data loss on leader failure
- Stale reads

---

# 5.4 Replication log

Leader sends a stream of changes to followers.

Possible mechanisms:

- Statement-based
- Write-ahead/log-based
- Logical row-level changes

Important idea:

> Replicas need an ordered stream of changes they can replay.

---

# 5.5 Replication lag

Example:

~~~text
Leader:    A B C D E
Follower:  A B C
~~~

Follower is behind.

This can produce **eventual consistency** and stale reads.

---

# 5.6 Read-after-write consistency

User writes:

~~~text
x = 10
~~~

Immediately reads from a stale replica:

~~~text
x = 5
~~~

Bad user experience.

Solutions can include:

- Read from leader for recently written data
- Track user's last-write position/version
- Route reads to sufficiently up-to-date replicas

---

# 5.7 Monotonic reads

A user should not observe time going backward.

Bad:

~~~text
Read 1 → version 10
Read 2 → version 8
~~~

Once a client has seen a newer version, subsequent reads should not return an older version.

---

# 5.8 Consistent prefix reads

If operations have causal order:

~~~text
A → B → C
~~~

A reader should not observe:

~~~text
B
~~~

without seeing A first.

Important for systems where events are causally ordered.

---

# 5.9 Follower failure

Usually a follower can recover by obtaining the changes it missed.

~~~text
Leader log:
A B C D E F

Follower before failure:
A B C

After recovery:
A B C D E F
~~~

---

# 5.10 Leader failure / failover

Typical steps:

1. Detect leader failure
2. Choose a new leader
3. Reconfigure clients
4. Make followers follow the new leader
5. Deal with old leader when it returns

Hard questions:

- Which replica is most up-to-date?
- What about writes not replicated?
- What if old leader comes back?
- Can two nodes believe they are leader?
- How do clients discover the new leader?

---

# 5.11 Split brain

Two nodes independently believe they are leader.

~~~text
        Leader A
        /      \
     writes   writes

        Leader B
~~~

Can cause conflicting writes/corruption.

Prevent with mechanisms such as:

- fencing
- consensus/coordination
- leases (with careful failure semantics)
- robust failover design

---

# 5.12 Multi-leader replication

Multiple nodes accept writes.

~~~text
       Leader A  <---->  Leader B
             \            /
              \          /
                Leader C
~~~

Useful for:

- Multi-datacenter systems
- Geographically distributed writes
- Offline clients / replicas
- Collaborative applications

Main problem:

> **Conflicting concurrent writes.**

Example:

~~~text
Replica A: name = Alice
Replica B: name = Bob
~~~

Both writes are locally valid.

---

# 5.13 Conflict resolution

Possible approaches:

### Last-write-wins (LWW)

Choose one version based on timestamp/order.

Simple, but may discard valid updates.

### Application-defined merge

Business logic determines the winner/merge.

### Data-type-specific merge

Some data structures can merge concurrent changes deterministically.

This leads to:

> **CRDTs (Conflict-free Replicated Data Types).**

---

# 5.14 Leaderless replication

No single leader.

Client sends writes to multiple replicas.

~~~text
              Client
             /  |  \
            v   v   v
           R1  R2  R3
~~~

Read also contacts multiple replicas.

Common mental model: Dynamo-style systems.

---

# 5.15 Quorum

Let:

- **N** = number of replicas
- **W** = replicas that must acknowledge a write
- **R** = replicas consulted for a read

Common quorum condition:

~~~text
W + R > N
~~~

Example:

~~~text
N = 3
W = 2
R = 2

2 + 2 > 3
~~~

Read and write quorums overlap.

### Important

Quorum does **not** magically eliminate conflicts.

Concurrent writes can still create multiple versions.

Quorum answers:

> How many replicas participate?

Conflict resolution answers:

> What should the final value be?

---

# 5.16 Concurrent writes

Suppose:

~~~text
Replica A:
x = 10

Replica B:
x = 20
~~~

and neither write causally depends on the other.

They are **concurrent**.

The system may need to preserve multiple versions until conflict resolution occurs.

---

# 5.17 Version vectors / vector clocks

Purpose:

> Track causality between versions.

Conceptually:

~~~text
Version V1 → [A:1, B:0]
Version V2 → [A:2, B:0]
Version V3 → [A:2, B:1]
~~~

If one version dominates another, it is causally newer.

If neither dominates:

> They are concurrent.

This is more useful than blindly comparing wall-clock timestamps.

---

# 5.18 Eventual consistency

If writes stop and the system continues propagating updates:

~~~text
Replica A ─┐
Replica B ─┼──→ eventually converge
Replica C ─┘
~~~

Eventual consistency means:

> Replicas eventually converge, assuming the system continues operating and no new conflicting updates keep arriving.

It does **not** mean:

> "Every read is immediately consistent."

---

# 5.19 Read repair

During a read, replicas may return different versions.

Example:

~~~text
R1 → version 5
R2 → version 5
R3 → version 3
~~~

The system can repair R3 while serving the read.

~~~text
R3 ← version 5
~~~

Reads therefore contribute to convergence.

---

# 5.20 Anti-entropy

Background process that compares replicas and synchronizes differences.

Useful when normal replication did not deliver an update.

### Merkle trees

Instead of comparing every record:

~~~text
             root hash
            /         \
        hash A       hash B
        /   \        /   \
      ...   ...    ...   ...
~~~

If subtree hashes match:

> The entire subtree is probably identical.

If they differ:

> Descend and find the differing region.

This reduces the amount of data that must be compared.

---

# 5.21 Hinted handoff

A replica is temporarily unavailable.

Instead of losing the write:

~~~text
R2 = DOWN

Client
  |
  +--> R1
  +--> R3
  |
  +--> temporary holder for R2
~~~

When R2 returns, the temporary holder forwards the missed update.

Mental model:

> **"I'll temporarily hold your data until you come back."**

---

# 5.22 Replication: consistency spectrum

Think in terms of requirements rather than "consistent/inconsistent."

~~~text
Stronger guarantees
      ↑
      |  linearizable / strong
      |
      |  read-after-write
      |  monotonic reads
      |  consistent prefix
      |
      |  eventual consistency
      ↓
Weaker guarantees
~~~

The exact guarantees and implementation mechanisms are separate concepts.

---

# 5.23 The most important replication distinctions

| Question | Concept |
|---|---|
| Who accepts writes? | Leader / multi-leader / leaderless |
| Does leader wait for replicas? | Sync / async |
| How far behind is replica? | Replication lag |
| What happens after leader failure? | Failover |
| Two leaders at once? | Split brain |
| Multiple independent writers? | Multi-leader |
| How many replicas participate? | Quorum |
| Concurrent versions? | Version vectors / conflict detection |
| How are conflicts resolved? | LWW / merge / application / CRDT |
| How do replicas catch up during reads? | Read repair |
| Background synchronization? | Anti-entropy |
| Temporary replica outage? | Hinted handoff |
| Eventual convergence? | Eventual consistency |

---

# 6. Cross-Chapter Connections

These connections are worth remembering because system-design interviews often combine chapters.

## LSM + SSTable + Bloom filter + Merkle tree

~~~text
Write
  ↓
Memtable
  ↓
SSTable
  ↓
Compaction
  ↓
Bloom filter → avoid unnecessary reads

Replication / anti-entropy
  ↓
Merkle tree → efficiently compare replicas
~~~

---

## Replication + storage engine

Replication does not replace the storage engine.

A replicated database still needs to store data using mechanisms such as:

- B-trees
- LSM trees
- WAL
- indexes
- SSTables

Think:

~~~text
Replication
    ↓
copies the logical changes/data
    ↓
each replica stores it locally
    ↓
using its storage engine
~~~

---

## Wide-column DB ≠ column-oriented analytics

This distinction is frequently confused.

### Wide-column / column-family

Data model:

~~~text
partition key
    ↓
rows / columns
~~~

Optimized around distributed access patterns and partitioning.

### Column-oriented analytics

Physical storage:

~~~text
Column A: A A A A
Column B: B B B B
Column C: C C C C
~~~

Optimized for analytical scans and aggregation.

**Same word "column"; very different idea.**

---

# 7. One-Page Mental Map

When recalling everything from the first five chapters:

~~~text
              DATA-INTENSIVE APPLICATIONS
                         |
       +-----------------+-----------------+
       |                 |                 |
   Data Model         Storage           Dataflow
       |                 |                 |
 SQL / Document      B-tree            Encoding
 Key-value            LSM              Schema evolution
 Wide-column          SSTable          RPC
 Graph                WAL              Message passing
 Joins                Bloom filter
                      Column store
                         |
                         v
                     Replication
                         |
          +--------------+--------------+
          |              |              |
        Leader       Multi-leader    Leaderless
          |              |              |
      sync/async      conflicts       quorum
      lag             resolution      N/R/W
      failover            |               |
      split brain      versions        convergence
                         |               |
                      CRDT/etc.     read repair
                                    anti-entropy
                                    hinted handoff
~~~

---

# 8. Rapid Recall Questions

Before an interview, close the document and answer these.

### Fundamentals

1. What makes a system reliable?
2. What are the main load parameters?
3. Latency vs throughput?
4. Why use percentiles?
5. What is tail latency?

### Data models

6. Relational vs document?
7. Why normalize?
8. Why denormalize?
9. What is a wide-column database?
10. Wide-column vs column-oriented storage?
11. When is a graph database useful?
12. Declarative vs imperative query?

### Storage

13. How does a hash index work?
14. Why append-only logs?
15. Why compaction?
16. What is an SSTable?
17. How does an LSM tree work?
18. Why use Bloom filters?
19. B-tree vs LSM?
20. What is WAL?
21. Why are secondary indexes expensive?
22. Row store vs column store?
23. OLTP vs OLAP?

### Encoding

24. Why is schema evolution difficult?
25. Backward vs forward compatibility?
26. Why are RPC calls different from local calls?
27. Where can data flow during a distributed system's lifetime?

### Replication

28. Why replicate?
29. Leader vs multi-leader vs leaderless?
30. Synchronous vs asynchronous replication?
31. What is replication lag?
32. Read-after-write consistency?
33. Monotonic reads?
34. Consistent prefix reads?
35. What makes failover difficult?
36. What is split brain?
37. Why use multi-leader?
38. What causes write conflicts?
39. What is quorum?
40. Explain N, R, W.
41. Does quorum eliminate conflicts?
42. What is a concurrent write?
43. What do version vectors tell us?
44. Read repair vs anti-entropy?
45. What is hinted handoff?
46. What is eventual consistency?
47. Where do CRDTs fit?
48. Why are Merkle trees useful for replica synchronization?

---

# 9. Final Recall Rules

If you remember only these:

1. **LSM:** write to memory → flush sorted immutable files → compact.
2. **SSTable:** sorted immutable key/value file.
3. **Bloom filter:** "definitely absent" vs "maybe present."
4. **B-tree:** page-oriented tree; update pages in place.
5. **Column store:** store columns together → efficient analytics.
6. **Wide-column:** distributed data model organized around partition/access patterns.
7. **WAL:** durable change log used for recovery.
8. **Replication:** multiple copies for availability, locality and scale.
9. **Leader:** one write authority.
10. **Multi-leader:** multiple write authorities → conflicts.
11. **Leaderless:** quorum-based reads/writes.
12. **Quorum:** N replicas, W write acknowledgements, R read replicas; commonly W + R > N.
13. **Quorum ≠ conflict resolution.**
14. **Version vectors:** reason about causality/concurrent versions.
15. **Read repair:** repair stale replicas during reads.
16. **Anti-entropy:** background replica synchronization.
17. **Merkle tree:** efficiently locate differences.
18. **Hinted handoff:** temporarily store data for an unavailable replica.
19. **Replication lag:** replicas can be behind.
20. **Eventual consistency:** replicas converge if updates stop and propagation continues.

> **Use this document for recall. Use DDIA for depth.**
