# DDIA — Deep Revision Guide Through Transactions

> **Book:** *Designing Data-Intensive Applications* — Martin Kleppmann (first edition).
> **Coverage:** Chapters 1–7: Reliable, Scalable, and Maintainable Applications; Data Models and Query Languages; Storage and Retrieval; Encoding and Evolution; Replication; Partitioning; Transactions.
> **Purpose:** Reconstruct the important ideas after reading. This is a revision guide, not a replacement for the book. It emphasizes mechanisms, execution flows, examples, trade-offs, and distinctions that are easy to confuse.
>
> **How to use:** Read a section, close it, explain the mechanism aloud, then use the recall questions at the end. Do not memorize isolated definitions; remember the problem each technique solves and the cost it introduces.

---

# 0. The mental model of the first seven chapters

A data-intensive application is one where the central challenge is managing data: storing it, retrieving it, moving it, processing it, and keeping it correct as load and failures increase.

The first seven chapters build a connected picture:

```text
Application requirements
        |
        v
Reliability, scalability, maintainability
        |
        v
Data model — what does the data mean?
        |
        v
Storage engine — how are bytes laid out and retrieved?
        |
        v
Encoding — how do processes exchange and evolve data?
        |
        v
Replication — how are copies kept up to date?
        |
        v
Partitioning — how is data spread across machines?
        |
        v
Transactions — how are concurrent changes kept correct?
```

Keep these questions separate:

| Concern | Main question |
|---|---|
| Data model | How should the application represent entities and relationships? |
| Storage engine | How are records, indexes, and updates physically stored? |
| Encoding | How can different processes and versions understand the same data? |
| Replication | How do multiple copies receive changes and handle divergence? |
| Partitioning | How is the dataset divided across nodes? |
| Transactions | How do related operations behave under failure and concurrency? |

A distributed database combines several of these concerns. Replication and partitioning are not substitutes for storage engines or transactions; they solve different dimensions of the problem.

---

# 1. Reliable, Scalable, and Maintainable Applications

## 1.1 Reliability: correct behavior despite faults

A system is reliable when it continues to provide the expected service, including when components fail.

Distinguish:

- **Fault:** one component deviates from its expected behavior.
- **Failure:** the system as a whole stops providing the required service.
- **Fault tolerance:** design that prevents a component fault from becoming a system-wide failure.

Typical fault sources:

- Hardware faults: disk, memory, power, network interface.
- Software faults: bugs, resource leaks, unexpected inputs.
- Process faults: crash, deadlock, out-of-memory termination.
- Human faults: incorrect configuration, accidental deletion, bad deployment.
- Dependency faults: unavailable database, slow downstream API, DNS or identity provider issue.
- Overload: the system receives more work than it can handle.

A fault-tolerant system does not assume faults can be eliminated. It limits their impact and recovers safely.

### Hardware faults vs software faults

Hardware faults are often handled through redundancy:

- RAID or replicated storage.
- Multiple machines.
- Redundant power and network paths.
- Replicas and failover.

Software faults are harder because the same bug may affect every replica. Running identical buggy software on five machines does not necessarily provide protection. Useful techniques include:

- Careful input validation.
- Process isolation.
- Monitoring and alerting.
- Rollbacks and staged deployments.
- Testing and property checking.
- Designing components to fail independently.

### Human errors

People are a major source of outages. Useful defenses include:

- Safe defaults.
- Preview/dry-run modes.
- Permission boundaries.
- Audit logs.
- Reversible operations.
- Backups and tested restoration.
- Staged rollouts and quick rollback.

### Reliability is not just uptime

A service may be reachable but return incorrect results. Correctness matters alongside availability.

A database that responds quickly with corrupted or stale data is not reliable for a workload that requires correct data.

## 1.2 Scalability: behavior as load grows

Scalability is not a single property or a claim that a system is “scalable.” It asks how the system behaves when a particular load parameter increases.

Describe load concretely:

- Requests per second.
- Read/write ratio.
- Number of active users.
- Number of concurrent connections.
- Payload size.
- Data growth per day.
- Cache hit ratio.
- Fan-out to downstream services.
- Background job or event rate.

A useful load description might be:

```text
Peak:             20,000 requests/sec
Read/write ratio:  9:1
New data:          300 GB/day
Concurrent users:  500,000
Target:            p99 < 250 ms
```

The point is not the particular numbers. The point is to make design decisions against a workload rather than vague scale claims.

### Latency, response time, and throughput

- **Latency:** time spent waiting for an operation or request to complete.
- **Response time:** what the client experiences end-to-end, including service time, queueing, network delays, and downstream work.
- **Throughput:** amount of work completed per unit time, such as requests/sec or MB/sec.

Latency and throughput are related but not interchangeable. A system can have high throughput and poor tail latency, or low latency at low load but collapse under high concurrency.

### Why averages are misleading

Averages hide the slowest requests. Use percentiles:

- p50: median; half the observations are at or below this value.
- p95: 95% are at or below this value.
- p99: 99% are at or below this value.
- p99.9: 99.9% are at or below this value.

Example:

```text
p50   = 25 ms
p95   = 90 ms
p99   = 400 ms
p99.9 = 2.5 s
```

The average may look acceptable while a meaningful fraction of users experiences seconds of delay.

For a simple sorted sample of N observations, one nearest-rank convention uses:

```text
rank = ceil(P × N)
```

where P is the percentile expressed as a fraction. Real monitoring systems may use interpolation, histograms, HDR histograms, t-digests, or other approximations, so exact results can differ.

**Tail latency and fan-out:** if one request waits for many downstream calls, the slowest dependency can dominate the overall response time. Even if each dependency is usually fast, the chance that at least one is slow increases with fan-out.

## 1.3 Load parameters and performance

A system should be described using the parameters that actually drive resource use.

Examples:

- A read-heavy workload may benefit from caching and read replicas.
- A write-heavy workload may stress log append, compaction, replication, and indexes.
- Large analytical scans need different storage and execution strategies from point lookups.
- High fan-out magnifies tail latency.
- Large payloads stress bandwidth, memory, serialization, and garbage collection.

A useful design process is:

1. Identify the workload.
2. Quantify its important parameters.
3. Identify the bottleneck resource.
4. Change the architecture or implementation.
5. Measure again under representative load.

## 1.4 Maintainability

Three concerns:

### Operability

Can the team keep the system healthy?

- Monitoring, logs, metrics, and tracing.
- Deployment and rollback.
- Backup and restore.
- Capacity planning.
- Debugging and incident response.
- Automation and predictable operations.

### Simplicity

Can engineers understand the system and reason about its behavior?

Good abstractions hide accidental complexity. They should not hide essential behavior or create layers that make failures harder to diagnose.

### Evolvability

Can the system adapt as requirements change?

- Modular design.
- Clear contracts.
- Schema evolution.
- Backward/forward compatibility.
- Automated tests.
- Small, reversible changes.

**Mental model:** reliability is about correct service under faults; scalability is about behavior under growing load; maintainability is about keeping the system understandable and changeable.

---

# 2. Data Models and Query Languages

The data model determines how the application represents entities, relationships, and the operations performed on them. It influences query shape, storage layout, consistency boundaries, and how easily requirements evolve.

## 2.1 Relational model

A relational database represents data as relations (commonly shown as tables), containing rows and columns.

Example:

```text
Customers
id | name
1  | Alice

Orders
id | customer_id | amount
9  | 1           | 100
```

The relationship is represented using a key, such as `Orders.customer_id` referring to `Customers.id`.

Strengths:

- Joins and many-to-many relationships.
- Declarative querying.
- Constraints and referential integrity.
- Transactions across multiple rows/tables.
- Flexible ad-hoc queries.

### SQL is declarative

A query describes **what result is required**, not the exact algorithm used to obtain it.

```sql
SELECT c.name, o.amount
FROM customers c
JOIN orders o ON o.customer_id = c.id
WHERE c.id = 1;
```

The database optimizer can choose a nested-loop join, hash join, sort-merge join, index access, or another execution plan.

### Joins: local vs distributed cost

Inside one database, the optimizer can use indexes, memory, and join algorithms.

In a distributed database, the two sides may be on different nodes:

```text
Node A: customer records
Node B: order records
          |
          v
   exchange / shuffle
          |
          v
         join
```

Moving large amounts of data over the network can dominate the cost. Distributed systems therefore often design data placement around known access patterns or deliberately denormalize.

### Normalization

Normalization reduces unnecessary duplication and update anomalies.

If customer details are copied into every order, changing the customer's address requires updating many records. A normalized model stores the customer once and references it.

### Denormalization

Denormalization deliberately duplicates or precomputes data to make reads cheaper.

Benefits:

- Fewer joins.
- Less runtime coordination.
- Faster read paths.
- Better locality for common access patterns.

Costs:

- More storage.
- More complicated writes.
- Multiple copies can disagree.
- Update propagation and repair become application concerns.

Do not treat normalization or denormalization as universally correct. Choose based on access patterns and consistency needs.

## 2.2 Document model

A document database stores an aggregate as a document, often JSON-like:

```json
{
  "customer_id": 1,
  "name": "Alice",
  "orders": [
    {"id": 9, "amount": 100}
  ]
}
```

The mental model is: store an object-shaped aggregate together when it is usually read and updated together.

Good fit when:

- Data naturally forms a tree or aggregate.
- Related information is commonly fetched together.
- The schema varies between records.
- Joins are not central to the workload.

Potential problems:

- Deeply nested structures can be awkward.
- Many-to-many relationships are less natural.
- Duplicated data can become inconsistent.
- Large documents can make small updates expensive.
- A document boundary may not match every business transaction boundary.

A document model does not mean relationships disappear; they may be represented by embedded objects or references.

## 2.3 Relational vs document: the real trade-off

| Dimension | Relational | Document |
|---|---|---|
| Relationships | Natural joins and foreign keys | Embedding or references |
| Data shape | Tables and relations | Nested aggregates |
| Schema | Usually explicit | Often flexible |
| Many-to-many | Natural | Often more awkward |
| Locality | Depends on joins/indexes | Embedded data is colocated |
| Transactions | Mature multi-row/table support | Depends on database and scope |
| Common fit | Connected data and ad-hoc queries | Aggregate-oriented access |

Neither model is universally superior. The important question is how the application reads, writes, and relates the data.

## 2.4 NoSQL is a family, not one model

Common categories include:

- Key-value.
- Document.
- Wide-column / column-family.
- Graph.

Common motivations:

- Specialized access patterns.
- Horizontal distribution.
- Flexible data representation.
- High throughput.
- Availability or geographic requirements.

“NoSQL” does not imply “no schema,” “no transactions,” or “automatically scalable.” Each database has specific guarantees and constraints.

## 2.5 Wide-column / column-family model

Cassandra- and Bigtable-style systems organize data around partition keys and ordered columns/rows. The model is designed for distributed access patterns.

For a Cassandra-style primary key:

```sql
PRIMARY KEY ((customer_id), order_date)
```

- **Partition key:** `customer_id`; determines the partition. Partition placement maps the partition to node(s).
- **Clustering key:** `order_date`; orders rows within the partition and supports efficient range access there.

Conceptually:

```text
customer_id = 10
  |
  +-- Jan 1 -> order A
  +-- Jan 5 -> order B
  +-- Feb 2 -> order C
```

A composite partition key can contain multiple fields:

```sql
PRIMARY KEY ((country, customer_id), order_date)
```

Here `(country, customer_id)` is the partition key and `order_date` is the clustering key.

**Query-driven modeling:** start with the queries, choose a partition key that distributes data and colocates the records needed together, then use clustering order to narrow scans within the partition.

A distributed database may not support arbitrary joins or global scans efficiently. Data models are therefore often designed around known query patterns.

**Important distinction:** wide-column is a distributed data model; column-oriented storage is a physical layout for analytical scans. The shared word “column” does not make them the same concept.

## 2.6 Graph model

A graph represents:

- Vertices/nodes: entities.
- Edges: relationships.
- Properties: information attached to nodes or edges.

Useful when relationships and traversal are central:

- Social networks.
- Recommendations.
- Fraud rings.
- Routing.
- Knowledge graphs.

The advantage is that traversing relationships is a first-class operation rather than reconstructing every path through repeated joins.

Graph data can be queried declaratively too; graph databases are not simply a different syntax for relational tables.

## 2.7 Declarative vs imperative languages

- **Declarative:** state what result is wanted; the engine chooses the execution plan.
- **Imperative:** specify the steps or algorithm used to produce the result.

SQL is primarily declarative. This separation allows the database optimizer to change the physical execution strategy without changing the query's intended result.

## 2.8 MapReduce

MapReduce is a batch-processing model:

```text
Input records
     |
     v
    Map
     |
     v
Shuffle / group by key
     |
     v
   Reduce
     |
     v
  Output
```

- Map transforms input records into key/value pairs.
- Shuffle groups values by key and moves data to the appropriate reducers.
- Reduce combines values for each key.

The shuffle is an important network and disk cost. MapReduce is suited to large batch jobs, but its rigid stages and materialized intermediate output can be less convenient for iterative or interactive processing.

## 2.9 Schema-on-write vs schema-on-read

- **Schema-on-write:** validate/transform data into a defined structure before storing it. Common in relational systems.
- **Schema-on-read:** store data with less upfront structural enforcement and interpret it when consumed. Common in data-lake and heterogeneous-data workflows.

Schema-on-read moves some complexity from ingestion to consumers; it does not eliminate schema decisions.

---

# 3. Storage and Retrieval

This chapter moves from logical data models to the physical structures used to find and update data.

The central question:

> How does a logical read/write become efficient disk and memory operations?

## 3.1 Two broad storage-engine families

```text
Storage engines
  |
  +-- Log-structured
  |     +-- append-only log
  |     +-- hash index
  |     +-- LSM tree / SSTables
  |
  +-- Page-oriented
        +-- B-tree
```

The categories emphasize different write strategies. Real engines can combine techniques.

## 3.2 Append-only log and hash index

A simple key-value store can append every update to a log:

```text
A=1
B=2
A=3
C=4
```

A hash index maps each key to the offset of its latest value:

```text
A -> offset of A=3
B -> offset of B=2
C -> offset of C=4
```

Benefits:

- Writes are sequential appends.
- Simple implementation.
- Fast point lookup when the index is in memory.

Limitations:

- The log grows indefinitely without cleanup.
- Hash indexes consume memory.
- Hash indexes do not efficiently support range scans.
- Large datasets may exceed memory for the complete index.

## 3.3 Compaction

Old values become obsolete when a key is updated:

```text
A=1
A=2
A=3
B=4
```

If only the latest value matters, compaction can produce:

```text
A=3
B=4
```

Compaction removes obsolete versions and reclaims space. It must preserve versions still needed by snapshots, transactions, or recovery rules.

## 3.4 SSTable

**SSTable = Sorted String Table.**

An SSTable is an immutable file containing key/value records sorted by key.

```text
A -> 10
B -> 20
C -> 30
D -> 40
```

Why sorted?

- Range scans can seek near the beginning of a range and scan forward.
- Files can be merged efficiently.
- Sparse indexes can point to approximate key positions.
- Compaction can use merge-sort-like processing.

Why immutable?

- Existing files are not modified in place.
- New data creates new files.
- Background compaction produces replacement files.
- Readers can safely use stable file contents.

An SSTable is a file/storage structure, not the entire LSM tree.

## 3.5 LSM tree

**Log-Structured Merge-Tree:** writes accumulate in memory, are flushed as immutable sorted files, and files are merged in the background.

### Write path

```text
Client write
    |
    v
WAL (durability / recovery)
    |
    v
Memtable (in memory, ordered)
    |
    | when full
    v
Immutable SSTable on disk
    |
    v
Background compaction
```

The WAL allows the database to reconstruct in-memory state after a crash. The memtable provides an ordered in-memory structure; when it reaches a threshold, it is flushed as an SSTable.

### Read path

A read may check:

1. The newest in-memory memtable.
2. Immutable memtables being flushed.
3. Relevant SSTables, usually newest to oldest or by level.
4. Index and filter metadata to avoid unnecessary file access.

The engine must choose the newest visible version and account for tombstones/deletions.

### Compaction

Because SSTables are sorted, the engine can merge them similarly to merge sort:

```text
SSTable 1: A=1, C=3, E=5
SSTable 2: B=2, C=30, D=4

                 |
                 v
Merged: A=1, B=2, C=30, D=4, E=5
```

The newer C=30 replaces C=3 when the engine can safely discard the older version.

### LSM trade-offs

- **Write amplification:** a record may be rewritten through multiple compactions.
- **Read amplification:** a lookup may inspect multiple files/levels.
- **Space amplification:** old and new versions can coexist temporarily.
- **Compaction resource use:** background work consumes disk bandwidth, CPU, and I/O budget.

LSM engines are often attractive for write-heavy workloads, but compaction strategy and read patterns matter.

## 3.6 Bloom filters

A Bloom filter is a probabilistic data structure used to test set membership.

Its answer is:

- **Definitely not present**, or
- **Possibly present**.

It can have false positives, but a correctly implemented Bloom filter has no false negatives for inserted elements.

```text
Bloom says NO  -> skip this SSTable
Bloom says YES -> check the SSTable; it may be a false positive
```

It reduces unnecessary disk reads. It does not return the value and does not prove that a key is present.

## 3.7 Compaction strategies

Two broad approaches:

- **Size-tiered:** merge files of similar sizes. Can reduce write cost in some workloads but may leave more files to check.
- **Leveled:** organize files into levels with controlled/non-overlapping key ranges within a level. Can improve read behavior but may rewrite data more frequently.

The exact trade-off depends on implementation and workload. Think in terms of read amplification, write amplification, space amplification, and compaction I/O.

## 3.8 B-trees

B-trees are page-oriented structures. Data is organized into fixed-size pages/blocks, and updates modify pages rather than creating only new sorted files.

```text
              Root page
              /       \
        Internal      Internal
         /   \          /   \
       Leaf  Leaf      Leaf  Leaf
```

A lookup traverses from root through internal pages to a leaf. High branching factor keeps tree height small.

Approximate lookup complexity is logarithmic in the number of records, with the branching factor determined by page size and key/pointer size.

B-tree characteristics:

- Efficient point lookups.
- Efficient range scans because leaves are ordered.
- Updates can modify pages in place.
- WAL or another recovery mechanism protects against partial page updates/crashes.
- Page splits/merges and maintaining indexes create write work.

## 3.9 B-tree vs LSM

| Dimension | B-tree | LSM |
|---|---|---|
| Main write idea | Modify pages | Append/flush sorted files, merge later |
| Read path | Tree traversal | Memtable plus relevant SSTables |
| Range scans | Ordered leaves | Sorted files and merge |
| Write amplification | Page/index updates and WAL | Compaction rewrites |
| Read amplification | Usually predictable | May inspect multiple files/levels |
| Background work | Page maintenance | Compaction is central |

Neither always wins. Workload, data size, read/write ratio, storage device, compaction, and latency requirements matter.

## 3.10 Transactions and storage-engine recovery

A storage engine needs to survive crashes during writes. A write-ahead log records intended changes before the corresponding data pages are safely updated.

```text
Write log record
       |
       v
Make log durable as required
       |
       v
Update data page / index
       |
       v
Checkpoint / recovery metadata
```

After a crash, recovery replays required log records and handles incomplete operations according to the engine's protocol.

## 3.11 WAL — Write-Ahead Log

The invariant is: the log record describing a change must be persisted before the corresponding data page is allowed to rely on that change being durable.

WAL supports:

- Crash recovery.
- Redo of committed changes.
- Undo/rollback where the engine uses undo information.
- Efficient sequential logging.

WAL is not the same as replication. A local WAL is primarily a recovery mechanism; a database may also ship log records to replicas.

## 3.12 Secondary indexes

A primary key identifies the main record. A secondary index supports lookups by another attribute.

Example:

```text
Primary key: user_id
Secondary index: email -> user_id / record location
```

Secondary indexes make additional query patterns efficient, but every relevant write may need to update each index. More indexes mean more write work, storage, and consistency/recovery bookkeeping.

## 3.13 Full-text search and inverted indexes

An inverted index maps terms to documents/records:

```text
"java"  -> doc1, doc4, doc9
"redis" -> doc2, doc4
```

This is different from a normal ordered index on one scalar field. It is designed to retrieve documents containing terms and may include tokenization, normalization, positions, and scoring metadata.

## 3.14 Column-oriented storage

Column-oriented storage is a physical layout that stores values from the same column together.

Row-oriented:

```text
row1: A1 B1 C1 D1
row2: A2 B2 C2 D2
row3: A3 B3 C3 D3
```

Column-oriented:

```text
A: A1 A2 A3
B: B1 B2 B3
C: C1 C2 C3
D: D1 D2 D3
```

If an analytical query needs only B and D from billions of rows, a column store can avoid reading A and C.

Benefits:

- Reads only required columns.
- Similar values often compress well.
- Efficient vectorized processing.
- Good for scans and aggregations.

Costs/limitations:

- Updating an entire row can be less natural.
- Reconstructing a full record can require reading multiple column files.
- Transactional point updates are not always the primary optimization target.

### OLTP vs OLAP

- **OLTP:** many small transactions, point lookups, inserts, updates, and low-latency record operations.
- **OLAP:** large scans, aggregations, grouping, and analytical queries.

Row-oriented storage is often a natural fit for OLTP; column-oriented storage is often a natural fit for OLAP.

**Do not confuse:** wide-column/column-family database model vs physical column-oriented analytical storage.

---

# 4. Encoding and Evolution

Data moves between processes and persists across software versions. In memory, an application may use objects, structs, and pointers; on disk or the network, data must be represented as bytes.

- **Encoding / serialization / marshalling:** structured values to bytes.
- **Decoding / deserialization / unmarshalling:** bytes back to structured values.

The format must answer: how are fields represented, how are types identified, how does a reader find boundaries, and what happens when writer and reader versions differ?

## 4.1 Character encoding is not data serialization

UTF-8 is a character encoding. It maps Unicode text to bytes. It does not describe objects, arrays, fields, numbers, or nested structure.

```text
Application object
       |
       v
JSON serialization
       |
       v
JSON text
       |
       v
UTF-8 character encoding
       |
       v
Bytes
```

JSON is a data format; UTF-8 is one way to encode its text as bytes.

UTF-8 uses leading-bit patterns to indicate whether a code point occupies 1, 2, 3, or 4 bytes. It does not prepend a separate length byte to every character.

## 4.2 JSON

JSON is self-describing text:

```json
{"name":"Alice","age":32,"active":true}
```

Field names and textual values are present in each document. This makes JSON easy to inspect and interoperable, but often verbose.

JSON parsers must interpret textual representations and infer/convert types according to the format and language's rules. Number precision and integer range can differ across languages and runtimes.

## 4.3 BSON

BSON is a binary document representation commonly associated with MongoDB.

Conceptually it contains:

```text
document length
[type][field name][encoded value]
[type][field name][encoded value]
...
terminator
```

BSON carries explicit type information and supports types such as integer widths, double, Decimal128, date, binary, and ObjectId.

It is not simply “compressed JSON.” It is a typed binary document format and still includes field names.

## 4.4 Numbers and precision

A number's textual appearance does not guarantee identical interpretation across languages.

- Binary floating point represents many decimal fractions approximately.
- Integer types have finite ranges.
- JavaScript's common Number type is IEEE 754 binary64 and cannot exactly represent every large integer.
- Decimal types such as Java BigDecimal represent decimal arithmetic differently.
- BSON Decimal128 is designed for decimal precision use cases.

When exchanging numbers, consider range, precision, scale, rounding, and how each language maps the wire value into its runtime type.

## 4.5 Schema-based binary formats

Formats such as Protocol Buffers and Thrift use schemas and compact field identifiers rather than repeating textual field names in each payload.

A conceptual Protobuf message:

```proto
message Person {
  string name = 1;
  int32 age = 2;
}
```

The wire payload uses field numbers and wire types. The reader needs the schema (or generated code based on it) to interpret those numbers correctly.

Benefits:

- Compact encoding.
- Explicit types.
- Generated code.
- Stronger contracts between services.

Costs:

- Schema management.
- Compatibility rules must be followed.
- Payload is not self-describing in the same way as JSON.
- Debugging raw bytes is less convenient.

## 4.6 Field numbers are identities

In Protobuf/Thrift-style formats, field identifiers are part of the contract.

Do not casually reuse a deleted field number for a different meaning. Old data may still contain that identifier and old/new readers may interpret it incorrectly.

Renaming a field while keeping its field number can be wire-compatible in formats where the numeric ID is the wire identity, but generated source APIs and JSON mappings may still be affected.

## 4.7 Avro

Avro is schema-driven and does not encode field names or type information in every record in the same way as JSON. The writer and reader schemas are important to decoding.

A key idea is **writer schema vs reader schema**:

```text
Writer schema
     |
     v
Serialized bytes
     |
     v
Reader schema + schema resolution
     |
     v
Reader's record
```

The reader resolves differences between the schema used to write the data and the schema expected by the reader.

This makes schema evolution explicit and central to the format.

## 4.8 Backward and forward compatibility

Suppose a system has old and new versions running simultaneously during a rolling deployment.

- **Backward compatibility:** new code can read data written by old code.
- **Forward compatibility:** old code can read data written by new code.

Compatibility is about readers and writers coexisting, not just whether a schema file parses.

Typical questions:

- What if a field is missing?
- What if a new field appears?
- Can an old reader ignore unknown fields?
- Is a default value defined?
- Can a type be promoted safely?
- Can a field be removed without breaking old stored data?

The exact rules differ by format. Do not assume every format handles unknown fields or defaults identically.

## 4.9 Schema evolution and database migrations

Schema changes are not instantaneous in a distributed application. Old and new application instances may run at the same time, and old data may remain stored for years.

A safe migration often follows an expand-and-contract approach:

1. Add a new field/representation while preserving the old one.
2. Deploy readers that understand both.
3. Deploy writers that populate the new representation.
4. Backfill old records if required.
5. Verify all consumers have migrated.
6. Remove the old representation only after it is no longer needed.

This is a deployment protocol, not merely a DDL statement.

## 4.10 Dataflow through databases

One process writes data and another reads it later:

```text
Application A -> database -> Application B
```

The stored representation becomes a contract between different software versions. A new writer may create data that an old reader still needs to understand.

## 4.11 Dataflow through services (REST/RPC)

One service sends data directly to another:

```text
Service A -> request/response protocol -> Service B
```

Both services may be deployed independently. Compatibility must be considered for request fields, response fields, types, errors, and behavior.

RPC can make a remote call look like a local function call, but remote calls have additional failure modes:

- Network delay and timeout.
- Partial failure.
- Retry ambiguity.
- Serialization/deserialization.
- Version mismatch.
- Authentication and authorization.
- Duplicate delivery or repeated execution.

A remote call is not equivalent to an in-process method call.

## 4.12 Dataflow through asynchronous messages

```text
Producer -> message broker -> consumer
```

The producer and consumer need not be online at the same time. Messages can be buffered and processed later.

The message format is a long-lived contract. A consumer may process messages produced by an older version of the service, so compatibility and schema evolution matter.

## 4.13 RPC and HTTP/2

RPC is a communication model, not a single wire protocol. gRPC commonly uses HTTP/2.

HTTP/2 supports multiplexing: multiple logical streams can share one established TCP connection.

```text
One TCP connection
  +-- stream A
  +-- stream B
  +-- stream C
```

This avoids opening a separate TCP connection for every concurrent RPC. It does not remove the initial connection establishment, and TCP-level loss can still affect streams sharing the connection.

## 4.14 Encoding is not compression

- **Encoding:** defines how values are represented.
- **Compression:** reduces the number of bytes required.

A compact binary format is not automatically a compression algorithm. A payload can be encoded using Protobuf and then compressed separately.

## 4.15 Format comparison

| Format | Field names in each payload? | External schema needed? | Main idea |
|---|---|---|---|
| JSON | Yes | No | Readable, self-describing text |
| BSON | Yes | No, for basic decoding | Typed binary document |
| Protobuf | No; field numbers | Yes | Compact tagged binary |
| Thrift | No; field IDs | Yes | Compact tagged binary |
| Avro | No | Yes | Schema-driven binary and writer/reader resolution |

---

# 5. Replication

Replication means keeping copies of the same data on multiple nodes.

Why replicate?

- Availability and fault tolerance.
- Read scalability.
- Geographic locality and lower read latency.
- Redundancy and recovery.

The trade-off is that copies can temporarily disagree. The system must define how updates propagate, which reads are allowed, and how conflicts are handled.

## 5.1 Three broad replication models

```text
Replication
  +-- Single-leader
  +-- Multi-leader
  +-- Leaderless
```

These differ primarily in who accepts writes and how replicas coordinate.

## 5.2 Single-leader replication

```text
             Client
                |
                v
              Leader
              /    \
             v      v
         Follower  Follower
```

Writes go to the leader. The leader sends changes to followers. Followers may serve reads depending on consistency requirements.

Benefits:

- One write authority simplifies ordering.
- Common and conceptually straightforward.
- Replication log can be applied by followers.

Costs:

- Leader can become a bottleneck.
- Failover is difficult.
- Asynchronous replication creates lag and possible data loss on failover.
- Reads from followers can be stale.

## 5.3 Synchronous vs asynchronous replication

**Synchronous:** leader waits for required replica acknowledgement(s) before acknowledging the write.

- Can strengthen durability and consistency, depending on what is acknowledged and how many replicas are required.
- Increases latency.
- Replica/network failure can block writes or reduce availability.

**Asynchronous:** leader acknowledges before followers necessarily catch up.

- Lower write latency.
- Leader can continue when followers are behind.
- Followers may serve stale data.
- A leader failure can lose writes not yet replicated.

Real systems often use hybrid policies rather than a simple all-sync/all-async choice.

## 5.4 Replication logs

A leader needs to transmit changes to followers. Common approaches include:

- Statement-based replication.
- Write-ahead-log shipping.
- Logical row-level changes.

The essential idea is an ordered stream of changes that followers can apply. The format and ordering semantics matter for correctness and recovery.

## 5.5 Replication lag

Example:

```text
Leader:    A B C D E F
Follower:  A B C
```

The follower has not applied D, E, F.

Consequences:

- Stale reads.
- Read-after-write anomalies.
- Different users seeing different states.
- Failover complexity if the most recent writes exist only on the old leader.

## 5.6 Read-after-write consistency

A user writes `x = 10`, then reads from a lagging follower and sees `x = 5`.

Possible strategies:

- Read the user's recently written data from the leader.
- Track a replication position/version and read from a replica that has caught up.
- Keep a short-lived session affinity policy.
- Use stronger coordination when the business operation requires it.

The guarantee is typically scoped to the user's own writes, not necessarily all writes by all users.

## 5.7 Monotonic reads

A client should not move backward in the version history.

Bad sequence:

```text
Read 1 -> version 10
Read 2 -> version 8
```

Once a client has observed a newer version, subsequent reads should not return an older version.

This may require session routing or version tracking.

## 5.8 Consistent prefix reads

If updates are causally ordered:

```text
A -> B -> C
```

A reader should not see B without seeing A first. The guarantee preserves a sensible prefix of the causal/logical order.

## 5.9 Follower failure and catch-up

A follower that temporarily fails can usually recover by obtaining the changes it missed, provided the leader retains enough history.

```text
Leader log:    A B C D E F
Follower had:  A B C
After catch-up: A B C D E F
```

If required log history has been discarded, the follower may need a fresh snapshot or full copy.

## 5.10 Leader failure and failover

Typical stages:

1. Detect the old leader is unavailable.
2. Select a new leader.
3. Reconfigure followers.
4. Redirect clients.
5. Fence or safely handle the old leader if it returns.

Hard questions:

- Which replica is most up to date?
- Were acknowledged writes replicated?
- Can two nodes believe they are leader?
- How do clients find the new leader?
- What happens to the old leader's divergent history?

Failover is a correctness problem, not merely a process restart.

## 5.11 Split brain and fencing

Split brain occurs when two nodes independently believe they are leader and both accept writes.

This can produce conflicting histories.

Defenses include:

- Consensus/coordination.
- Leases with carefully defined failure semantics.
- Fencing tokens or epochs.
- Ensuring stale leaders cannot continue issuing accepted writes.

A fencing token is a monotonically increasing generation/epoch. Downstream resources reject operations from an older generation, preventing a paused or partitioned former leader from acting after a new leader has been elected.

## 5.12 Multi-leader replication

Multiple nodes accept writes and replicate changes to each other.

Useful for:

- Multi-datacenter deployments.
- Geographically distributed writes.
- Offline clients.
- Collaborative applications.

Main challenge: two leaders can accept concurrent conflicting changes.

Example:

```text
Leader A: name = Alice
Leader B: name = Bob
```

Both writes may be locally valid. The system needs a conflict-resolution policy.

## 5.13 Conflict resolution

Possible approaches:

- Last-write-wins (LWW): select a value using timestamps/order. Simple, but can discard valid concurrent updates and depends on timestamp assumptions.
- Application-defined merge: business logic decides how to combine changes.
- Data-type-specific merge: data structures designed to merge concurrent updates deterministically.
- Preserve multiple versions and let a client/application resolve them.

CRDTs (Conflict-free Replicated Data Types) are data types with merge properties designed to make replicas converge under defined conditions. They do not make arbitrary business conflicts disappear.

## 5.14 Leaderless replication

No single leader accepts all writes. A client sends operations to multiple replicas and reads from multiple replicas.

Dynamo-style systems are a common mental model.

The system uses replica counts and acknowledgement/read thresholds, but quorum alone does not solve every conflict or ordering problem.

## 5.15 Quorum: N, W, R

- **N:** number of replicas for the data.
- **W:** number of replicas that must acknowledge a write.
- **R:** number of replicas consulted for a read.

A common overlap condition is:

```text
W + R > N
```

Example:

```text
N = 3
W = 2
R = 2
W + R = 4 > 3
```

For a fixed replica set, the read set and write set must overlap.

What quorum gives:

- A read can contact at least one replica that acknowledged the write, under the assumed replica-set and acknowledgement model.

What quorum does not automatically give:

- No concurrent writes.
- No conflicts.
- Linearizability.
- A globally ordered value.
- Latest-value reads under every failure and versioning scenario.

Quorum, versioning/causality, and conflict resolution are separate mechanisms.

## 5.16 Concurrent writes and versions

Two writes are concurrent when neither causally depends on the other.

A system may need to preserve multiple versions rather than immediately discarding one. The system must distinguish:

- One version superseding another.
- Two versions being concurrent.
- How concurrent versions are merged or resolved.

Quorum does not serialize concurrent writes.

## 5.17 Version vectors / vector clocks

A version vector tracks how much history from each replica a version has observed.

Example:

```text
V1 = [A:1, B:0]
A makes another update:
V2 = [A:2, B:0]
B observes V2 and updates:
V3 = [A:2, B:1]
```

V3 dominates V2 because it includes V2's history plus B's update.

Compare:

```text
V2 = [A:2, B:0]
V4 = [A:1, B:1]
```

Neither dominates the other; they are concurrent.

Version vectors help reason about causality. They do not decide the business meaning of how concurrent values should be merged.

Wall-clock timestamps are not a reliable substitute for causal history: a later timestamp does not prove that one operation observed another.

## 5.18 Eventual consistency

If updates stop and propagation continues, replicas eventually converge.

It does not mean every read immediately returns the latest value. It is a convergence guarantee under assumptions about continued communication and repair.

## 5.19 Read repair

During a read, the system may discover that one replica is stale:

```text
R1 -> V5
R2 -> V5
R3 -> V3
```

It can return the appropriate version and repair R3.

Read repair uses normal reads as an opportunity to synchronize replicas. A key that is never read may not be repaired through read repair alone.

## 5.20 Anti-entropy and Merkle trees

Anti-entropy is background synchronization between replicas. It does not require a client read.

Comparing every key is expensive. Merkle trees summarize groups of records using hashes:

- Matching subtree hashes mean that region can be skipped.
- Different subtree hashes mean the system descends into that region.
- Repeating this locates the differing ranges efficiently.

Mental model: Merkle tree = locate which parts differ without comparing every record individually.

## 5.21 Hinted handoff

If a replica is temporarily unavailable, another node may temporarily store a hint that an update belongs to that replica. When the replica returns, the hint is forwarded.

This improves availability during temporary failures, but is not a substitute for full anti-entropy or durable backup. Hints can expire or be insufficient for long outages, depending on implementation.

## 5.22 Consistency guarantees are distinct

- **Linearizability:** operations appear to take effect atomically in one real-time order.
- **Read-after-write:** a client's later read sees its own write or something newer.
- **Monotonic reads:** a client does not move backward in observed versions.
- **Consistent prefix:** a reader does not observe effects in an order that violates causal/logical ordering.
- **Eventual consistency:** replicas converge if updates stop and propagation continues.

Do not collapse these into one vague “strong vs weak consistency” label. Ask which guarantee the application needs.

---

# 6. Partitioning

Replication creates multiple copies of data. Partitioning (sharding) divides a dataset into smaller subsets and distributes those subsets across nodes.

The goal is to spread data and workload so one machine does not need to store or process everything.

## 6.1 Partitioning vs replication

- **Partitioning:** different nodes own different subsets of the data.
- **Replication:** multiple nodes store copies of the same data.

A distributed database commonly uses both:

```text
Dataset
  |
  +-- Partition 1 -> replicas on nodes A and B
  +-- Partition 2 -> replicas on nodes B and C
  +-- Partition 3 -> replicas on nodes C and D
```

Partitioning helps scale storage and throughput. Replication helps availability and read locality.

## 6.2 Why partition?

- Dataset exceeds one machine's storage.
- Read/write throughput exceeds one node.
- Workload can be parallelized.
- Data locality can reduce network traffic.

Partitioning introduces costs:

- Routing requests to the correct partition.
- Rebalancing as nodes are added/removed.
- Cross-partition queries.
- Cross-partition transactions.
- Skew and hot partitions.

## 6.3 Partitioning by key range

Assign contiguous ranges of keys to partitions.

```text
Partition 1: A ... F
Partition 2: G ... M
Partition 3: N ... Z
```

Advantages:

- Efficient range scans.
- Natural ordering by key.
- Nearby keys are colocated.

Disadvantages:

- Hotspots if access is concentrated in one range.
- Sequential or time-based keys can overload the newest range.
- Rebalancing ranges requires careful split/merge decisions.

## 6.4 Partitioning by hash of key

Apply a hash function to the key and distribute hash ranges across partitions.

```text
key -> hash(key) -> partition
```

Advantages:

- More even distribution for well-behaved hash functions.
- Reduces hotspots caused by naturally ordered keys.

Disadvantages:

- Range queries become difficult because adjacent keys hash to unrelated partitions.
- Hashing can destroy locality.
- A hot individual key remains hot even if the overall distribution is balanced.

## 6.5 Skew and hot spots

A partitioning scheme can be mathematically balanced but operationally uneven.

- **Skew:** some partitions hold or process much more data/work than others.
- **Hot key:** one key receives a disproportionate share of requests.
- **Hot partition:** one partition receives disproportionate traffic.

Possible mitigations:

- Better partition key.
- Split hot keys using controlled salting/sharding.
- Cache heavily read values.
- Replicate hot read data.
- Separate high-volume tenants.
- Rate-limit or queue hot writes.

Salting can distribute load, but complicates reads and aggregation because the application may need to query multiple salted keys and combine results.

## 6.6 Secondary indexes in partitioned databases

A secondary index can be organized in two broad ways.

### Local (document-partitioned) index

Each partition maintains an index over its own records.

A query by a non-partition key may need to contact every partition:

```text
Query
  +--> Partition 1 local index
  +--> Partition 2 local index
  +--> Partition 3 local index
  |
  v
Merge results
```

This is scatter/gather. It is simple to maintain locally but can be expensive for reads.

### Global (term-partitioned) index

The index itself is partitioned by indexed term/value.

A query can route to the partition responsible for the term, but writes may need to update a different partition from where the record lives.

This can make index maintenance distributed and potentially asynchronous.

Trade-off:

- Local index: write locality, potentially scatter/gather reads.
- Global index: targeted reads, more complicated distributed index writes/maintenance.

## 6.7 Rebalancing

Rebalancing moves partitions when:

- A node is added.
- A node is removed.
- Storage or traffic becomes uneven.
- A partition grows too large.

A good strategy aims to minimize data movement and avoid overloading the cluster during migration.

## 6.8 Rebalancing strategies

### Hash mod N

```text
partition = hash(key) mod N
```

Simple, but adding/removing a node changes many mappings because N changes.

### Fixed number of partitions

Create more logical partitions than physical nodes, then assign partitions to nodes. Adding a node means moving some partitions rather than remapping every key.

### Dynamic partitioning

Split partitions when they grow too large and merge when they become too small. Common in range-partitioned systems.

### Partitioning proportional to nodes

Some systems maintain a fixed number of partitions per node and redistribute when cluster membership changes.

Each strategy trades simplicity, movement, and balance.

## 6.9 Consistent hashing

Consistent hashing maps both keys and nodes into a hash space (often represented as a ring). A key is assigned to the next node clockwise, according to the scheme.

When a node is added or removed, only a subset of keys need to move in the idealized model, rather than almost all keys as with naive hash mod N.

Virtual nodes (vnodes) give each physical node multiple positions on the ring, improving distribution and making rebalancing more flexible.

Consistent hashing reduces movement; it does not automatically solve hot keys, data skew, replica placement, or cross-partition queries.

## 6.10 Request routing

A request must find the partition that owns the key.

Common approaches:

- Client knows the partitioning scheme.
- A routing/proxy layer knows the mapping.
- A coordinator/service discovers the owner.

The mapping must remain correct during rebalancing. Stale routing information needs redirects, retries, or metadata refresh.

## 6.11 Parallel query execution

Partitioning can allow scans and aggregations to run in parallel:

```text
Query
  |
  +--> Partition 1 -> partial result
  +--> Partition 2 -> partial result
  +--> Partition 3 -> partial result
                    |
                    v
                combine
                    |
                    v
                 result
```

The benefit depends on data distribution, query shape, network cost, and whether one partition dominates the work.

**Mental model:** partitioning spreads data and work; the hard parts are choosing the key, avoiding skew, routing requests, rebalancing safely, and managing operations that span partitions.

---

# 7. Transactions

Transactions group multiple reads and writes into one logical unit. They help applications handle partial failures and concurrent access without implementing every recovery and coordination mechanism themselves.

The central question:

> How do we make concurrent operations on shared data behave correctly, even when applications, databases, or machines fail?

## 7.1 Why transactions?

Consider a transfer of 100 from account A to account B:

```text
A = A - 100
B = B + 100
```

If the process crashes after debiting A but before crediting B, money disappears. A transaction makes both database updates atomic.

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 100
WHERE id = 'A';

UPDATE accounts
SET balance = balance + 100
WHERE id = 'B';

COMMIT;
```

Either both changes commit or neither does.

Transactions also address:

- Application crashes.
- Database crashes.
- Network failures during operations.
- Concurrent modifications.
- Mid-operation constraint or validation failures.

A local database transaction does not automatically include an external payment gateway, email service, or another database. Those require distributed workflows such as outbox/saga patterns and idempotency.

## 7.2 ACID

| Property | Meaning |
|---|---|
| Atomicity | All operations in the transaction happen, or none do |
| Consistency | A transaction preserves application-defined invariants |
| Isolation | Concurrent transactions do not interfere beyond the chosen isolation guarantees |
| Durability | Committed changes survive failures covered by the database's durability configuration |

### Atomicity

Atomicity is about partial failure. If one operation fails, the transaction should not leave earlier operations permanently applied.

Common mechanisms include WAL, undo information, redo information, and recovery procedures.

### Consistency

Consistency does not mean the database automatically understands all business rules.

If the invariant is `balance >= 0`, a database constraint can enforce it. More complex invariants may need application logic, locking, serializable isolation, or a data-model change.

Transactions help preserve invariants; developers must define them.

### Isolation

Isolation concerns concurrent transactions: what one transaction can observe while another transaction is executing.

### Durability

A commit should survive the failures covered by the database's guarantees. Actual durability depends on settings such as log flushing and replication acknowledgement. A system that acknowledges before data is safely persisted/replicated may provide weaker guarantees than expected.

## 7.3 Single-object vs multi-object transactions

A single-object operation modifies one row/document/key. Databases can often provide atomic operations for it.

Example:

```sql
UPDATE counters
SET value = value + 1
WHERE id = 'page_views';
```

The increment is performed atomically by the database.

Multi-object transactions modify several rows, tables, or documents together:

```text
Update account A
Update account B
Insert transfer history
Update transfer status
Commit
```

Atomicity across several objects is harder. Some NoSQL systems provide atomicity only within one document/key or a limited scope. Do not infer multi-object transaction support from single-object atomicity.

## 7.4 Concurrency anomalies

When transactions overlap, weak concurrency control can produce anomalies. Learn each by recognizing the schedule.

### Dirty read

Transaction B reads a value written by A before A commits.

```text
A: write balance = 500 (uncommitted)
B: read balance = 500
A: ROLLBACK
```

B observed a value that never became committed database state. B might make a decision based on a value that disappears.

### Dirty write

One transaction overwrites another transaction's uncommitted write.

```text
Initial X = 100
A: write X = 200 (uncommitted)
B: write X = 300
A: rollback
```

Dirty writes complicate rollback and recovery. Databases generally prevent them using write locks or equivalent mechanisms.

| Anomaly | Meaning |
|---|---|
| Dirty read | Read another transaction's uncommitted data |
| Dirty write | Overwrite another transaction's uncommitted data |

### Read skew / non-repeatable read

A transaction observes different committed versions during its execution.

Initial state:

```text
Account A = 500
Account B = 500
Total = 1000
```

Transaction A transfers 100:

```text
A = 400
B = 600
COMMIT
```

Transaction B reads A before the commit and B after it:

```text
B reads A = 500
A commits transfer
B reads B = 600
B observes total = 1100
```

The report combines values from different database states.

A **non-repeatable read** is the simpler case: a transaction reads the same row twice and gets different values because another transaction committed an update between the reads.

Read skew often involves multiple related records; non-repeatable read describes a repeated row read changing.

### Lost update

Two transactions read the same value, calculate independently, and overwrite each other's result.

```text
Initial counter = 10

A reads 10; calculates 11
B reads 10; calculates 11
A writes 11
B writes 11

Expected = 12
Actual   = 11
```

Common prevention techniques:

1. Atomic database operation, e.g. `SET value = value + 1`.
2. Explicit row lock before read-modify-write.
3. Optimistic compare-and-set using a version column.
4. Database conflict detection/abort.

Example optimistic update:

```sql
UPDATE accounts
SET balance = 900, version = 5
WHERE id = 'A' AND version = 4;
```

If zero rows are updated, the version changed and the application must handle the conflict.

Lost update is not the same as dirty write: both transactions can read committed data, yet one overwrites the other's intended result.

### Write skew

Two transactions read the same set of data, make decisions based on that shared snapshot, and write different rows. Since they do not write the same row, ordinary write-write conflict detection may not detect the logical conflict.

Example: at least one doctor must remain on call.

Initial:

```text
Doctor A: on_call = true
Doctor B: on_call = true
```

Concurrent execution:

```text
Transaction A reads A=true, B=true
Transaction B reads A=true, B=true

A decides B remains on call -> sets A=false
B decides A remains on call -> sets B=false

Both write different rows and commit
```

Final state has nobody on call. The invariant is violated.

| Lost update | Write skew |
|---|---|
| Same object is overwritten | Different objects are modified |
| One update disappears | Both updates may survive |
| Atomic update/version check often helps | Protect the broader invariant |
| Counter increment | Minimum staffing rule |

Prevention:

- Serializable isolation.
- Lock all rows involved in the invariant.
- Introduce a coordination row both transactions must update.
- Use a database constraint when the invariant can be expressed declaratively.

### Phantom read

A repeated predicate query returns a different set of rows because another transaction inserts/deletes rows matching the predicate.

```sql
SELECT * FROM bookings WHERE room_id = 10;
```

First execution returns five rows. Another transaction inserts a booking for room 10 and commits. The repeated query now returns six rows.

- Non-repeatable read: an existing row changes.
- Phantom read: the set of rows matching a condition changes.

This matters for rules over a collection, such as “no overlapping booking exists” or “at most N active reservations.”

## 7.5 Isolation levels

Isolation is not simply on/off. Levels prevent different subsets of anomalies, and database implementations differ.

### Read Uncommitted

Weakest standard level. Dirty reads may be possible. Other guarantees depend on implementation.

### Read Committed

Guarantees that reads do not expose another transaction's uncommitted data and that conflicting writes are controlled.

In PostgreSQL, Read Committed uses a new snapshot for each statement:

```text
Transaction A: SELECT -> value 100
Transaction B: UPDATE -> value 50; COMMIT
Transaction A: SELECT -> value 50
```

Repeated reads can see newly committed values. Therefore Read Committed does not prevent read skew/non-repeatable reads.

### Snapshot Isolation

A transaction reads from a consistent snapshot, usually established at the start of the transaction.

If another transaction commits after the snapshot is established, the existing transaction continues seeing the older committed versions.

This prevents read skew for snapshot reads.

Snapshot isolation commonly detects conflicting concurrent writes to the same row (often described as first-committer-wins), but it does not prevent every anomaly. In particular, write skew can still occur because transactions may update different rows.

### Repeatable Read terminology

SQL isolation names and actual implementation guarantees do not map identically across all databases.

For example, PostgreSQL Repeatable Read provides snapshot isolation. MySQL InnoDB Repeatable Read also uses MVCC, with additional details around consistent reads, locking reads, and range locking.

Never assume that the same isolation-level name means identical behavior in every database.

## 7.6 MVCC — Multiversion Concurrency Control

MVCC is an implementation technique: maintain multiple versions of a record so transactions can read the version visible to their snapshot.

Conceptually:

```text
Row X:
  Version 1 -> value 1000
  Version 2 -> value 900
```

An older snapshot may see version 1; a newer snapshot after commit may see version 2.

Benefits:

- Readers often do not block writers.
- Writers often do not block snapshot readers.
- Consistent snapshots are possible.
- Read-heavy workloads can benefit.

Costs:

- Old versions consume storage.
- Obsolete versions need cleanup.
- Long-running transactions can prevent cleanup of versions they may still need.
- Version visibility and vacuum/garbage-collection work add overhead.

PostgreSQL uses MVCC and tuple visibility information. Old tuple versions are eventually reclaimed by vacuum when no active transaction needs them.

**Important:** MVCC is not an isolation level. It is a technique used to implement isolation behavior.

## 7.7 Serializability

Serializable isolation is the strongest common isolation guarantee discussed here.

A concurrent execution is serializable if its result is equivalent to some serial execution in which transactions run one after another in a valid order.

The database does not have to literally execute everything one at a time. It must ensure the outcome is equivalent to a serial ordering.

Three approaches:

1. Actual serial execution.
2. Two-phase locking (2PL).
3. Serializable Snapshot Isolation (SSI).

## 7.8 Actual serial execution

The database runs one transaction at a time within an execution domain.

```text
A -> B -> C -> D
```

This avoids overlapping transactions in that domain and therefore avoids many concurrency anomalies.

It can be practical when:

- Transactions are short.
- Data fits in memory.
- One thread can process the workload quickly enough.
- Contention is manageable.

Limitations:

- A single execution thread can bottleneck.
- Long-running transactions block others.
- Disk I/O or network waits waste the serial execution slot.
- CPU-heavy work delays the queue.

### Stored procedures and round trips

A client-side transaction may repeatedly alternate between database and application:

```text
Application -> SQL -> DB
Application <- result <- DB
Application computes
Application -> next SQL -> DB
...
```

Network round trips make transactions longer.

A stored procedure moves transaction logic into the database, reducing communication overhead:

```text
Application -> procedure call
                    |
                    v
             DB executes logic
                    |
                    v
                  commit
```

Trade-offs: database-specific code, more complex testing/debugging, and business logic split between application and database.

### Partitioning serial execution

Independent partitions can execute transactions concurrently:

```text
Partition 1 -> Thread 1 (serial within partition)
Partition 2 -> Thread 2 (serial within partition)
Partition 3 -> Thread 3 (serial within partition)
```

Transactions confined to one partition can be processed without coordinating with others. A transaction that touches multiple partitions needs coordination and is more expensive.

## 7.9 Two-phase locking (2PL)

2PL is a concurrency-control protocol. It is not two-phase commit.

Two phases:

1. **Growing phase:** acquire locks; do not release locks yet.
2. **Shrinking phase:** release locks; do not acquire new locks after releasing the first lock.

### Shared and exclusive locks

- **Shared (S) lock:** used for reading; multiple transactions can hold shared locks simultaneously.
- **Exclusive (X) lock:** used for writing; conflicts with shared and exclusive locks.

| Existing lock | New S lock | New X lock |
|---|---|---|
| S | Allowed | Blocked |
| X | Blocked | Blocked |

Example:

```text
A obtains X-lock on account A
A updates account A
B requests X-lock on account A -> waits
A commits and releases lock
B acquires lock and continues
```

2PL can provide serializability when the required data/predicate conflicts are properly locked.

### Costs of 2PL

- Blocking and lock contention.
- Deadlocks.
- Lock-management overhead.
- Long transactions hold locks and reduce concurrency.

### Deadlocks

```text
Transaction A holds X, waits for Y
Transaction B holds Y, waits for X
```

Neither can proceed. The database detects the cycle and aborts one transaction, which releases its locks. Applications must handle deadlock victims safely.

### Predicate locks

A row lock protects an existing row. But a query such as:

```sql
SELECT * FROM bookings WHERE room_id = 10;
```

also depends on the condition `room_id = 10`. Another transaction could insert a new matching row.

A predicate lock conceptually protects all rows matching a condition, including rows that do not yet exist. It prevents a concurrent insert from changing the query's result.

### Index-range / next-key locks

An implementation can protect an index range rather than maintaining a general predicate lock.

For example, a query on `age BETWEEN 20 AND 30` can lock the relevant index range so inserts into that range are blocked.

MySQL InnoDB uses next-key locking for certain locking reads/range operations.

**Recall:** row lock = existing record; predicate/range lock = logical set of records matching a condition.

## 7.10 Serializable Snapshot Isolation (SSI)

SSI aims to combine snapshot-based reads with serializability.

Instead of blocking every conflicting operation as 2PL does, SSI allows snapshot execution and tracks dependencies. If a dangerous dependency structure could produce a non-serializable execution, the database aborts a transaction.

### Read-write dependencies / anti-dependencies

A read-write dependency occurs when transaction A reads a version that transaction B later replaces. A's read is logically before B's write in a serialization order.

These dependencies can form cycles. SSI tracks relevant dependencies and looks for dangerous structures that can lead to a serialization cycle.

It does not abort every transaction that has any dependency; it detects patterns that threaten serializability.

Conceptual flow:

```text
Transactions read snapshots
          |
          v
Transactions execute concurrently
          |
          v
Database tracks read/write dependencies
          |
          v
Dangerous structure detected?
          |
       yes -> abort one transaction
       no  -> allow commit
```

Benefits:

- Snapshot readers generally do not block writers.
- Writers generally do not block ordinary snapshot reads.
- Concurrent execution is possible.
- Serializable outcomes are preserved through conflict detection and aborts.

Costs:

- Dependency-tracking overhead.
- Transactions may be aborted and need retry.
- High contention can increase abort rates.
- Long-running transactions complicate dependency tracking.

PostgreSQL supports Serializable Snapshot Isolation with:

```sql
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

The application must handle serialization failures and retry the entire transaction from fresh reads.

## 7.11 Compare serializability approaches

| Property | Serial execution | 2PL | SSI |
|---|---|---|---|
| Main idea | One transaction at a time per execution domain | Concurrent work controlled by locks | Concurrent snapshots with dependency tracking |
| Conflict response | Avoid overlap | Block conflicting operations | Abort dangerous transaction |
| Reader/writer blocking | No overlap in same serial domain | Possible | Ordinary snapshot reads generally avoid it |
| Deadlocks | Not typical within one serial queue | Possible | Different failure model |
| Retry reasons | Application/transaction failures | Deadlock victim, etc. | Serialization failure |
| Parallelism | Across independent partitions | Across nonconflicting work | Across snapshot transactions |

## 7.12 Practical engineering implications

- Keep transactions short.
- Prefer atomic SQL updates over application-side read-modify-write.
- Use optimistic version checks where appropriate.
- Lock all records required to protect a business invariant.
- Understand the database's actual isolation implementation.
- Retry deadlock/serialization failures by restarting the complete transaction.
- Make retryable operations idempotent.
- Do not perform irreversible external side effects inside a transaction that may be retried.
- Use outbox/saga/idempotency patterns when a workflow crosses database and external-service boundaries.

---

# 8. Cross-chapter connections

## 8.1 Storage engine vs replication vs partitioning

```text
Logical database
     |
     +-- Storage engine
     |      B-tree / LSM / WAL / indexes
     |
     +-- Replication
     |      copies and propagation
     |
     +-- Partitioning
     |      distributes subsets of data
     |
     +-- Transactions
            atomicity and concurrency correctness
```

A replicated database still needs a local storage engine. A partitioned database may replicate each partition. Transactions may be local to one partition or require cross-partition coordination.

## 8.2 Wide-column vs column-oriented

- Wide-column: logical distributed data model, organized around partition and clustering keys.
- Column-oriented: physical storage layout, grouping values by column for analytical scans.

Same word, different layer.

## 8.3 Quorum vs serializability

Quorum determines how many replicas participate in a read/write and can create an overlap property. It does not by itself prevent write skew or guarantee a serializable order.

## 8.4 MVCC vs SSI

- MVCC maintains multiple versions and enables snapshot visibility.
- Snapshot isolation is a concurrency model built using snapshots.
- SSI adds dependency tracking/abort logic to provide serializability on top of snapshot-based execution.

## 8.5 Local transaction vs distributed workflow

A database transaction can atomically update rows within its transaction boundary. It cannot automatically roll back an email, payment gateway call, or another independent service.

For cross-service workflows, use explicit coordination patterns:

- Transactional outbox for reliable event publication.
- Idempotency keys for safe retries.
- Saga/compensation for multi-step workflows.
- Durable workflow state and observability.

---

# 9. High-value comparisons

| Do not confuse | Difference |
|---|---|
| Fault vs failure | Component deviation vs system-level inability to provide service |
| Latency vs throughput | Time per operation vs work per unit time |
| Average vs p99 | Central tendency vs tail behavior |
| Relational vs document | Relationship/join-oriented model vs aggregate-oriented document model |
| Wide-column vs column store | Distributed data model vs physical analytical layout |
| Schema-on-write vs schema-on-read | Validate before storage vs interpret at read time |
| Hash index vs SSTable | Key-to-location map vs sorted immutable file |
| SSTable vs LSM | File structure vs complete log-structured storage design |
| B-tree vs LSM | Page updates vs immutable files and compaction |
| WAL vs replication log | Local recovery log vs propagation of changes to replicas (may share implementation) |
| Encoding vs compression | Representation vs reducing byte count |
| UTF-8 vs JSON | Character encoding vs data format |
| JSON vs BSON | Text self-describing document vs typed binary document |
| Protobuf vs Avro | Tagged fields with field numbers vs schema-driven writer/reader resolution |
| Backward vs forward compatibility | New reader reads old data vs old reader reads new data |
| Replication vs partitioning | Multiple copies vs different subsets |
| Key-range vs hash partitioning | Preserves key order/locality vs distributes keys by hash |
| Local vs global secondary index | Per-partition index/scatter-gather vs index partitioned by indexed term |
| Quorum vs conflict resolution | Replica participation/overlap vs deciding between versions |
| Read repair vs anti-entropy | Repair during reads vs background synchronization |
| Read skew vs lost update | Inconsistent observations vs overwritten modification |
| Lost update vs write skew | Same object overwritten vs different objects violate shared invariant |
| Non-repeatable vs phantom read | Existing row changes vs predicate result set changes |
| Snapshot isolation vs serializable | Consistent snapshot vs equivalent to serial execution |
| MVCC vs isolation level | Versioning technique vs promised concurrency behavior |
| 2PL vs 2PC | Locking/concurrency control vs distributed atomic commit |
| Predicate lock vs row lock | Protects a condition/range vs existing row |
| 2PL vs SSI | Blocks conflicts vs tracks dependencies and aborts |
| Local transaction vs saga | Atomic DB boundary vs multi-step distributed workflow with compensation |

---

# 10. Rapid recall questions

Try to answer aloud before expanding the answer or reopening the chapter.

## Chapter 1 — Foundations

1. Fault vs failure?
2. What is fault tolerance?
3. Why do software faults differ from hardware faults?
4. What load parameters should describe a workload?
5. Latency vs response time vs throughput?
6. Why can averages hide a bad user experience?
7. What does p99 mean?
8. Why does fan-out worsen tail latency?
9. What are operability, simplicity, and evolvability?

## Chapter 2 — Data models

10. Relational vs document model?
11. Why normalize?
12. When denormalize?
13. Why can distributed joins be expensive?
14. What are key-value, document, wide-column, and graph models?
15. Partition key vs clustering key?
16. Why is Cassandra modeling query-driven?
17. Wide-column vs column-oriented storage?
18. Declarative vs imperative query?
19. MapReduce map/shuffle/reduce?
20. Schema-on-write vs schema-on-read?

## Chapter 3 — Storage and retrieval

21. How does an append-only log work?
22. What does a hash index map?
23. Why compact?
24. What makes an SSTable sorted and immutable?
25. Walk through the LSM write path.
26. Walk through the LSM read path.
27. Why does compaction happen?
28. Read/write/space amplification?
29. What can a Bloom filter say, and what can it not say?
30. How does a B-tree lookup work?
31. B-tree vs LSM?
32. What does WAL guarantee?
33. Why do secondary indexes make writes more expensive?
34. Why is column storage efficient for OLAP?
35. OLTP vs OLAP?

## Chapter 4 — Encoding and evolution

36. UTF-8 vs JSON?
37. Why is JSON self-describing?
38. What does BSON add?
39. Why do floating-point and decimal representations differ?
40. How do Protobuf field numbers work?
41. Why should field numbers not be reused?
42. How does Avro writer/reader schema resolution work?
43. Backward vs forward compatibility?
44. Why do rolling deployments make schema evolution difficult?
45. Encoding vs compression?
46. Why is RPC not equivalent to a local call?
47. What does HTTP/2 multiplexing provide?
48. Database vs service vs message dataflow?

## Chapter 5 — Replication

49. Why replicate?
50. Single-leader vs multi-leader vs leaderless?
51. Synchronous vs asynchronous replication?
52. What is replication lag?
53. Read-after-write vs monotonic reads?
54. What is consistent prefix?
55. Why is failover difficult?
56. What is split brain?
57. What is fencing?
58. Why does multi-leader create conflicts?
59. What are N, W, and R?
60. What does W + R > N provide?
61. Why does quorum not guarantee serializability?
62. What is a concurrent write?
63. What does a version vector tell us?
64. What does eventual consistency actually guarantee?
65. Read repair vs anti-entropy?
66. Why use a Merkle tree?
67. What is hinted handoff?

## Chapter 6 — Partitioning

68. Partitioning vs replication?
69. Key-range vs hash partitioning?
70. Why can time-based keys create hotspots?
71. What is a hot key?
72. How can salting help, and what cost does it introduce?
73. Local vs global secondary index?
74. What is scatter/gather?
75. Why does naive hash mod N move many keys when N changes?
76. How does consistent hashing reduce movement?
77. What are virtual nodes?
78. What are the main rebalancing strategies?
79. Why are cross-partition queries and transactions expensive?

## Chapter 7 — Transactions

80. Explain ACID without mixing consistency and isolation.
81. Single-object vs multi-object transaction?
82. Dirty read vs dirty write?
83. Read skew vs non-repeatable read?
84. Lost update vs write skew?
85. Non-repeatable read vs phantom read?
86. How does Read Committed work in PostgreSQL?
87. What is a statement-level snapshot?
88. How does Snapshot Isolation prevent read skew?
89. Why can Snapshot Isolation still permit write skew?
90. What is MVCC, and what operational cost does it introduce?
91. What does serializability mean?
92. How does serial execution work?
93. Why do stored procedures help short transactions?
94. How does partitioning enable parallel serial execution?
95. Explain 2PL growing and shrinking phases.
96. Shared vs exclusive lock?
97. How does a deadlock occur?
98. Why are predicate locks needed?
99. Predicate lock vs index-range/next-key lock?
100. What is a read-write anti-dependency?
101. How does SSI detect dangerous structures?
102. Why must an application retry a serialization failure from the beginning?
103. Why is 2PL different from 2PC?
104. Why can't a local transaction atomically include a payment gateway?

---

# 11. Final mental map

```text
DATA-INTENSIVE APPLICATIONS
|
+-- 1. Foundations
|     +-- Reliability / faults / failure
|     +-- Scalability / load / latency / throughput
|     +-- Maintainability
|
+-- 2. Data models
|     +-- Relational / joins / normalization
|     +-- Document / aggregate
|     +-- Key-value / wide-column / graph
|     +-- Declarative queries / MapReduce
|
+-- 3. Storage
|     +-- Hash index / append-only log
|     +-- SSTable / LSM / compaction / Bloom filter
|     +-- B-tree / WAL / secondary index
|     +-- Row store / column store / OLTP / OLAP
|
+-- 4. Encoding
|     +-- JSON / UTF-8 / BSON
|     +-- Protobuf / Thrift / Avro
|     +-- Schema evolution / compatibility
|     +-- Database / RPC / message dataflow
|
+-- 5. Replication
|     +-- Single-leader / multi-leader / leaderless
|     +-- Lag / read guarantees / failover / split brain
|     +-- Quorum / versions / conflict resolution
|     +-- Read repair / anti-entropy / Merkle / hints
|
+-- 6. Partitioning
|     +-- Key-range / hash
|     +-- Skew / hot keys
|     +-- Local/global indexes
|     +-- Rebalancing / consistent hashing
|
+-- 7. Transactions
      +-- ACID / single vs multi-object
      +-- Dirty read/write / skew / lost update / phantom
      +-- Read Committed / Snapshot Isolation / MVCC
      +-- Serializability
            +-- Serial execution
            +-- 2PL / locks / deadlocks / predicates
            +-- SSI / dependencies / abort and retry
```

## Final recall rules

1. **Reliability:** faults are inevitable; prevent a component fault from becoming a system failure.
2. **Scalability:** quantify the workload and measure tail latency, not only averages.
3. **Data model:** choose around entities, relationships, and access patterns.
4. **LSM:** memtable -> immutable SSTables -> compaction.
5. **B-tree:** page-oriented ordered index with in-place page updates.
6. **WAL:** durable log supports recovery.
7. **Column store:** read only required columns for analytical scans.
8. **Encoding:** format and schema are contracts between versions.
9. **Replication:** multiple copies improve availability/locality but can diverge.
10. **Quorum:** replica overlap is not conflict resolution or serializability.
11. **Partitioning:** spread data/work, but manage skew, routing, and rebalancing.
12. **Transactions:** atomicity handles partial failure; isolation handles concurrency.
13. **Snapshot Isolation:** consistent snapshot, but write skew can remain.
14. **MVCC:** multiple versions support snapshot visibility; old versions need cleanup.
15. **Serializable:** concurrent outcome must equal a valid serial execution.
16. **2PL:** block conflicts with locks.
17. **SSI:** detect dangerous dependencies and abort when necessary.
18. **Distributed workflows:** a local DB transaction does not include external services.

> **Use this guide to reconstruct the concepts. Return to DDIA when you need the full derivation, implementation details, or examples beyond this revision scope.**
