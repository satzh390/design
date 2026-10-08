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

- **Latency:** the delay between a request and its response; in practice, this is often used to mean the end-to-end delay.
- **Response time:** the total time the client observes from sending the request until receiving the response.
- For interviews, the key point is that **tail latency** can be much worse than the average.

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

### Joins — what actually happens

A join reconstructs a relationship that is stored separately.

~~~text
Customer:
id=1, Alice

Order:
customer_id=1, amount=100
~~~

The database matches customer.id = order.customer_id and produces Alice | 100.

In one database, the optimizer can choose hash join, sort-merge join, nested-loop join, indexes, and so on. SQL describes the result, not the algorithm.

At distributed scale, the two sides may live on different partitions:

~~~text
Node A: Customer rows
Node B: Order rows

        ↓ network/shuffle

      join
~~~

Moving large amounts of data between nodes can be expensive. This is one reason distributed NoSQL databases often prefer query-specific data placement or denormalization.

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

Think of Cassandra/Bigtable-style systems as distributed tables designed around the query pattern.

~~~text
Partition key
     ↓
partition (data placed together)
     ↓
rows ordered by clustering columns
     ↓
cells / columns
~~~

### Cassandra primary key: two different roles

~~~sql
PRIMARY KEY ((customer_id), order_date)
~~~

- **Partition key = customer_id** → determines which partition the rows belong to; partitioning/replication then maps that partition to one or more nodes.
- **Clustering key = order_date** → orders rows inside that partition and makes range/prefix queries efficient within that partition.

~~~text
customer_id = 10
    |
    +-- order_date = Jan 1 → order A
    +-- order_date = Jan 5 → order B
    +-- order_date = Feb 2 → order C
~~~

A query for customer_id = 10 and an order_date range can be efficient because the system first finds one partition and then uses clustering order inside it.

### Composite partition key

The partition key itself can contain multiple fields:

~~~sql
PRIMARY KEY ((country, customer_id), order_date)
~~~

Here:

~~~text
partition key = (country, customer_id)
clustering key = order_date
~~~

The full primary key therefore consists of a partition-key component plus zero or more clustering columns.

### Why query-driven modeling matters

A distributed database cannot cheaply perform arbitrary global joins/scans for every request.

~~~text
Known query
    ↓
choose partition key
    ↓
related rows land together
    ↓
query one/few partitions
    ↓
use clustering order to narrow the scan
~~~

**Interview rule:** For Cassandra-style databases, start from the queries/access patterns and design the table to make those queries efficient.

**Important:** wide-column/column-family storage is not the same thing as analytical column-oriented storage.

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

Interpret or apply the expected structure when reading the data.

Useful for heterogeneous / evolving data.

**Recall:** schema-on-write = enforce/transform before storage; schema-on-read = interpret when consumed.

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

**SSTable = Sorted String Table.**
An SSTable is an **immutable file whose records are sorted by key**.

~~~text
A → 10
B → 20
C → 30
D → 40
~~~

### Why sorted?

Sorted order makes range scans, merging, sparse indexing and compaction efficient.
For a range C–F, the engine can jump near C and scan forward instead of scanning the entire file.

### Why immutable?

The database does not update an existing SSTable in place. New SSTables are created and later merged during compaction. This makes reads and background merging simpler.

**Mental model:** SSTable = **sorted + immutable + disk-resident**. It is a storage structure/file, not the whole LSM tree.
---

# 3.4 LSM Tree

**LSM = Log-Structured Merge-Tree.**

The useful mental model is: **writes accumulate in memory, become immutable sorted files, and those files are continuously merged in the background.**

### Write path

~~~text
Client write
    ↓
Memtable (RAM)
    ↓ when full
immutable SSTable
    ↓
more SSTables accumulate
    ↓
background compaction
    ↓
fewer/larger SSTables
~~~

A WAL/durable log is commonly used so a write held in memory can be recovered after a crash.

### Read path

A lookup may check the current memtable and several SSTables. Indexes and Bloom filters reduce unnecessary file checks.

### How compaction works

Because SSTables are sorted, files can be merged like merge-sort:

~~~text
SSTable 1:        SSTable 2:
A=1               B=2
C=3               C=30
E=5               D=4

             ↓ merge

A=1
B=2
C=30   ← newer value
D=4
E=5
~~~

Old C=3 can be discarded when it is safe. The result is another sorted immutable SSTable.

### Why LSM is attractive

Writes avoid repeatedly modifying random disk pages. They can be accumulated and flushed as sorted files, which is often write-friendly.

### The price

- **Write amplification:** data may be rewritten during multiple compactions.
- **Read amplification:** a lookup may inspect several SSTables.
- **Space amplification:** old and new versions can coexist temporarily.
- **Compaction I/O:** background merging consumes disk/CPU resources.

**Mental model:** LSM = **memtable + immutable SSTables + background compaction**. The merge/compaction work is the price paid for write-friendly ingestion.
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

**Column-oriented storage** is a physical layout optimized for analytical workloads.

Row-oriented:
~~~text
Row1: A B C D
Row2: A B C D
Row3: A B C D
~~~

Column-oriented:
~~~text
A: A A A
B: B B B
C: C C C
D: D D D
~~~

If an analytical query needs only B and D from 1 billion rows, a column store can read B and D instead of loading every column.

### Why it is good for OLAP

- fewer irrelevant bytes read
- values in the same column often compress well
- efficient vectorized operations
- fast large aggregations

### Why row storage is usually better for OLTP

A transactional request often wants one complete record. Row storage keeps fields of a record together, making point reads and updates natural.

### Do not confuse

~~~text
Wide-column database
→ distributed data model / partitioning

Column-oriented database
→ physical storage layout / analytical scan optimization
~~~
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

Data must move between processes, machines, and storage systems. In memory, applications use objects/structs/pointers; on the wire or disk, data becomes a sequence of bytes.

**Encoding / serialization / marshalling:** converting structured data into bytes.

**Decoding / deserialization / unmarshalling:** converting bytes back into structured data.

The important questions are not only payload size, but also: Can another language read it? Does the decoder know the types/structure? Can readers and writers evolve independently? Can old and new versions coexist during rolling deployment?

---

## 4.1 UTF-8 is NOT a serialization format

**UTF-8 is a character encoding, not a document/data format.** It maps Unicode characters to bytes. A character can occupy 1–4 bytes.

UTF-8 itself knows nothing about objects, fields, field names, arrays, numbers, booleans, document length, or number of fields. Those concepts belong to a format such as JSON.

~~~text
Application object
      ↓
JSON serialization
      ↓
JSON text
      ↓
UTF-8 encoding
      ↓
bytes
~~~

**Recall:** JSON is the data format; UTF-8 is one way to encode the JSON text as bytes.

---

## 4.2 UTF-8 does NOT store a length byte before every character

Common misconception: because UTF-8 characters are variable length, every character has a separate length prefix. **False.**

UTF-8 uses bit patterns in the first byte to tell the decoder whether the character occupies 1, 2, 3, or 4 bytes.

~~~text
1 byte → 0xxxxxxx
2 bytes → 110xxxxx 10xxxxxx
3 bytes → 1110xxxx 10xxxxxx 10xxxxxx
4 bytes → 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
~~~

So UTF-8 does not add a separate length byte to every character.

---

## 4.3 JSON: self-describing but text-heavy

JSON carries structure and field names in the payload itself:

~~~json
{"name":"Sathish","age":32,"active":true}
~~~

The overhead comes from field names, punctuation, and textual representations of values. **It is not because UTF-8 adds a length prefix before each character.**

JSON is popular because it is human-readable, language-independent, and easy to debug, but it is often more verbose than schema-based binary formats.

---

## 4.4 BSON: binary document encoding

**BSON = Binary JSON**, commonly used by MongoDB. BSON is not simply "UTF-8 but smaller". It represents a document as a binary structure containing information such as:

~~~text
[document length]
[type][field name][value]
[type][field name][value]
...
[end]
~~~

BSON still carries field names, so it remains relatively self-describing.

### Why MongoDB uses BSON

1. **Typed values** — BSON has explicit types such as int32, int64, double, Decimal128, date, binary, and ObjectId.
2. **Binary structure** — the database can work with typed binary values instead of first interpreting a textual JSON representation.
3. **Document length/structure** — encoded length helps parsers know document boundaries and traverse the representation.
4. **Richer types than standard JSON** — for example Decimal128, binary data, ObjectId, and BSON dates.
5. **Good fit for MongoDB's document model.**

### BSON is NOT necessarily smaller than JSON

BSON can be larger for some documents because it stores type information, field names, and structural metadata. Its primary purpose is **not compression**.

**Mental model:**

~~~text
JSON + UTF-8
→ textual, self-describing representation

BSON
→ binary, typed, self-describing document representation
~~~

---

## 4.5 JSON numbers vs BSON numeric types

JSON defines a general numeric grammar: **number**. It does not define separate wire types such as int32, int64, float64, or Decimal128.

A particular JSON parser may choose how to represent a number internally. Many environments use IEEE-754 double precision, which cannot exactly represent every large integer or high-precision decimal.

So:

> A JSON number with many digits is not automatically a high-precision Decimal128 value.

BSON can explicitly represent types such as Int32, Int64, Double, and Decimal128. This matters when exact decimal precision is important, such as monetary values.

**Recall:** JSON says "number"; BSON can say "this is specifically Decimal128." 

---

## 4.6 Schema-based binary formats

Formats such as **Protocol Buffers (Protobuf), Apache Thrift, and Avro** can be much more compact because the reader and writer can rely on a schema.

Instead of repeatedly sending:

~~~text
"name" → "Sathish"
"age"  → 32
~~~

a Protobuf schema can define:

~~~protobuf
message Person {
  string name = 1;
  int32 age = 2;
}
~~~

The payload can use compact **field numbers/tags** rather than sending the complete field name each time.

**Progression:**

~~~text
JSON
→ field names + text structure

BSON
→ field names + binary types/structure

Protobuf / Thrift
→ field IDs + compact typed encoding

Avro
→ schema-driven encoding without per-field IDs in the normal binary encoding
~~~

---

## 4.7 Protobuf field tags / wire types

A Protobuf field has a **field number** and a **wire type**. Conceptually:

~~~text
tag + encoded value
~~~

Not every field is `tag + length + value`.

~~~text
varint field
→ tag + varint value

length-delimited field
→ tag + length + bytes

fixed32
→ tag + 4 bytes

fixed64
→ tag + 8 bytes
~~~

So a length prefix applies to **length-delimited** values, not every Protobuf field.

---

## 4.8 Why Protobuf/Thrift field IDs help schema evolution

Suppose:

~~~protobuf
message User {
  string name = 1;
  int32 age = 2;
}
~~~

Later add `string email = 3;`. Old readers can ignore the unknown field 3. New readers can handle old messages where field 3 is absent according to the format's presence/default semantics.

### Critical rule

**Never casually reuse an old field number for a different meaning.** When a Protobuf field is removed, reserving its number/name is a common safety practice so it cannot accidentally be reused.

**Recall:**

~~~text
Field number = stable identity
Field name   = human-readable label
~~~

The stable identity is what makes evolution robust.

---

## 4.9 Thrift

Thrift is another schema-based serialization/RPC ecosystem. A Thrift field carries a field ID/type and value in its binary protocols. Thrift supports structured types such as structs, lists, sets, and maps.

The key recall point is:

> **Stable field IDs allow fields to be added/removed while old and new versions can coexist, provided the schema-evolution rules are respected.**

---

## 4.10 Avro: the key difference

Avro is also schema-based, but unlike Protobuf/Thrift it does not normally put a field ID/tag beside every field in its binary encoding.

~~~text
Protobuf / Thrift
→ identify fields using stable IDs/tags

Avro
→ schema determines what the next encoded value means
~~~

This makes Avro very compact, but schema evolution is handled through **writer schema + reader schema + schema resolution**.

---

## 4.11 Avro: why deleting a field does NOT shift values incorrectly

A tempting but incorrect mental model is:

> "Avro is positional, so if I delete a field, every later value shifts and the reader assigns it to the wrong field."

**That is not how Avro schema evolution works.**

Avro has:

~~~text
Writer schema
      ↓
describes how bytes were written
      ↓
serialized bytes
      ↓
Reader schema
      ↓
reader
~~~

The reader uses the **writer schema and reader schema** together to resolve differences.

Example:

~~~text
Writer schema: name, age, email
Reader schema: name, age
~~~

The writer can encode `name → age → email`. The reader knows the writer schema, so it can consume the email value and ignore it because `email` is absent from the reader schema.

It does **not** blindly treat email's bytes as the next reader field.

---

## 4.12 Avro: adding a field

Suppose:

~~~text
Writer: name, age
Reader: name, age, email
~~~

The writer has no `email`. The reader can supply a value from a **default** specified in the reader schema when appropriate.

~~~text
email → default ""
~~~

**Recall:** defaults matter when the reader expects a field that is missing from the writer data.

---

## 4.13 Avro: deleting a field

Suppose:

~~~text
Old writer: name, age, email
New reader: name, age
~~~

The reader does not need `email`. Avro schema resolution can ignore fields that exist in the writer schema but not the reader schema.

**No tombstone/dummy field is required merely because the reader removed the field.**

---

## 4.14 Avro: reordering fields

Writer:

~~~text
name, age, email
~~~

Reader:

~~~text
email, name, age
~~~

The physical writer encoding still follows the writer schema. The reader uses schema resolution to match fields by schema name rather than assuming that the first physical value must be the first field in the reader schema.

Therefore, reordering fields in the reader schema does not by itself make values land in the wrong fields.

---

## 4.15 Avro evolution mental model

Remember:

~~~text
             Writer
               |
         Writer schema
               |
               v
        serialized bytes
               |
               v
         Reader schema
               |
             Reader
~~~

Useful recall rules:

- Reader field missing from writer → needs a suitable default/presence rule.
- Writer field missing from reader → reader can ignore it.
- Field matching is based on schema names/resolution, not simply reader position.
- Type changes are allowed only when a compatible schema-resolution/promotion rule exists.

---

## 4.16 Text vs self-describing binary vs schema-based binary

| Format | Field names in payload? | External schema required? | Main idea |
|---|---:|---:|---|
| JSON | Yes | No | Human-readable, self-describing text |
| BSON | Yes | No | Typed binary document |
| Protobuf | No; field numbers | Yes | Compact tagged binary |
| Thrift | No; field IDs | Yes | Compact tagged binary |
| Avro | No | Yes | Schema-driven binary + schema resolution |

**Important:** BSON and JSON carry enough metadata in each document to decode without an external application schema. Protobuf/Thrift/Avro rely much more heavily on an agreed schema.

---

## 4.17 Encoding is not compression

Do not mix these concepts:

~~~text
Encoding
→ represents data in a defined format

Compression
→ reduces the number of bytes needed to represent that data
~~~

A binary format can be compact without being a compression algorithm. Protobuf + gzip is still possible, for example.

---

## 4.18 RPC and HTTP/2

**RPC is a communication model, not a specific wire protocol.** gRPC commonly uses HTTP/2.

~~~text
Application
    ↓
gRPC / RPC
    ↓
HTTP/2
    ↓
TCP
    ↓
IP
~~~

### Why HTTP/2 helps RPC

HTTP/2 supports **multiplexing**:

~~~text
One TCP connection
    |
    +-- stream → RPC A
    +-- stream → RPC B
    +-- stream → RPC C
    +-- stream → RPC D
~~~

Multiple concurrent streams can share one TCP connection. Therefore many RPCs can reuse the same connection instead of opening a new TCP connection for every request.

But HTTP/2 does **not** eliminate the initial TCP connection establishment. It lets many streams share the established connection.

**Recall:** RPC ≠ HTTP/2. gRPC uses HTTP/2; RPC systems can use other transports.

---

## 4.19 The full encoding progression

~~~text
In-memory object
      ↓
Need bytes for network/storage
      ↓
JSON + UTF-8
      ↓
Human-readable, interoperable, but verbose
      ↓
BSON
      ↓
Binary typed document; still carries field names
      ↓
Protobuf / Thrift
      ↓
Schema + stable field IDs → compact payloads
      ↓
Avro
      ↓
Schema-driven encoding + writer/reader schema resolution
~~~

When asked "why binary/schema-based encoding?", remember: **binary alone is not the magic; avoiding repeated textual metadata and having an agreed schema are what enable compact, efficient representations.**

---

## 4.20 Schema evolution: interview checklist

When a schema changes, ask:

1. Can new code read old data? → **backward compatibility**
2. Can old code read new data? → **forward compatibility**
3. How are fields identified? → name / field ID / schema resolution
4. What happens when a field is missing? → default / absence semantics
5. What happens when an unknown field appears? → ignore / reject, depending on format
6. Can a field be renamed without losing identity? → depends on the format
7. Can a field's type change? → only if a compatible conversion/resolution exists

**Core idea:** schema evolution is about allowing different versions of readers and writers to coexist safely.

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

Leader waits for the required replica acknowledgement(s) before acknowledging the write.

Pros:

- Can provide stronger durability/consistency guarantees, depending on what the acknowledgement actually means and how many replicas are required

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
- **N** = total replicas
- **W** = replicas required to acknowledge a write
- **R** = replicas consulted for a read

A common quorum relationship is:
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

The write set and read set must overlap when **W + R > N** (for the normal fixed replica set for that key).

### What quorum gives you

It gives an **overlap property**: the read and write sets share at least one replica, so the read can encounter a replica that acknowledged the write.

### What quorum does NOT give you automatically

Quorum does not automatically mean:
- no concurrent writes
- no conflicts
- linearizability
- one globally ordered value

Example:
~~~text
Client A → R1,R2 : x=10
Client B → R2,R3 : x=20
~~~

Both writes may be accepted. R2 may observe both, while R1 and R3 temporarily differ.

Keep these separate:
~~~text
Quorum
→ which replicas participate?

Versioning
→ which version happened after which?

Conflict resolution
→ how do concurrent versions become one converged state?
~~~

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

The system may need to preserve multiple versions until it can determine their causal relationship and/or resolve the conflict.

**Important:** quorum does not serialize concurrent writes. If a conditional update must behave as one atomic, globally ordered operation, a stronger coordination mechanism is required (for example, a linearizable compare-and-set/consensus-backed operation).

---

# 5.17 Version vectors / vector clocks

A version vector records how much history from each replica a version has observed.

~~~text
V1 = [A:1, B:0]

A makes another update:
V2 = [A:2, B:0]

B observes V2 and updates:
V3 = [A:2, B:1]
~~~

V3 dominates V2 because it includes all history represented by V2 plus B's new update.

Now compare:
~~~text
V2 = [A:2, B:0]
V4 = [A:1, B:1]
~~~

Neither vector dominates the other, so V2 and V4 are concurrent.

Wall-clock timestamps tell you which timestamp is larger, but they do not reliably tell you whether one update actually observed another.

Version vectors distinguish:
~~~text
causal: V1 → V2

concurrent: V2 || V4
~~~

They detect/reason about causality; they do **not** decide how concurrent values should be merged.

---

# 5.18 Eventual consistency

If writes stop and the system continues propagating updates:

~~~text
Replica A ─┐
Replica B ─┼──→ eventually converge
Replica C ─┘
~~~

Eventual consistency means:

> If updates stop and propagation continues, replicas eventually converge to the same value/state.

It does **not** mean:

> "Every read is immediately consistent."

---

# 5.19 Read repair

Suppose a read contacts three replicas:
~~~text
R1 → V5
R2 → V5
R3 → V3
~~~

The system can return V5 and also repair R3 by sending V5 to it.

So: **read repair = use a normal read as an opportunity to repair a stale replica.**

It helps frequently accessed keys, but a key that is never read will not be repaired by read repair.

---

# 5.20 Anti-entropy

Anti-entropy is **background synchronization between replicas**. It does not require a client to read the key.

~~~text
Replica A ←→ Replica B
       compare state
             ↓
       synchronize differences
~~~

### Merkle trees

Comparing every key between huge replicas is expensive. A Merkle tree summarizes groups of keys using hashes:
~~~text
              root
             /    \
          hash     hash
          / \      / \
        ... ...  ... ...
~~~

If a subtree hash matches, that whole region can be skipped. If it differs, descend into that subtree until the differing region is found.

**Mental model:** Merkle tree = quickly locate which parts of two replicas differ, without comparing every record.

---

# 5.21 Hinted handoff

Suppose R2 is temporarily unavailable.

~~~text
R1 ← write
R3 ← write

R2 = unavailable
~~~

Another node can temporarily store a **hint** saying that the update belongs to R2.

~~~text
temporary holder
       ↓
      R2
~~~

When R2 returns, the temporary holder forwards the missed update.

**Mental model:** temporary storage on behalf of a failed replica. It improves availability, but adds recovery and synchronization work.

---

# 5.22 Replication: consistency guarantees

Do not treat consistency as one simple ladder where every guarantee is strictly stronger or weaker than every other one. They describe different properties.

- **Linearizability:** operations appear to take effect atomically in one real-time order.
- **Read-after-write:** after a client writes, its later read should see that write or something newer.
- **Monotonic reads:** once a client has seen a version, later reads should not go backward.
- **Consistent prefix:** a reader should not observe effects in an order that contradicts their causal/logical order.
- **Eventual consistency:** if updates stop and propagation continues, replicas eventually converge.

**Recall:** ask “What exact guarantee does the application need?” rather than simply saying “strong vs weak consistency.”

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
