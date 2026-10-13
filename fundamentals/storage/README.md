# Bloom Filter Implementation

This module provides a Java-based Bloom Filter implementation optimized for memory efficiency and fast lookups. It utilizes CRC32 and Adler32 hash functions for double hashing, ensuring a low false positive rate.

## 📦 Features

- **Memory Efficient:** Uses a `long[]` bit array to minimize memory usage.
- **Fast Lookups:** Implements double hashing with CRC32 and Adler32 for quick insertions and lookups.
- **Configurable Parameters:** Allows customization of expected elements, false positive probability, and number of hash functions.
- **Error Handling:** Throws exceptions if calculated bit array size exceeds `Integer.MAX_VALUE`.

## ⚙️ Usage

### Initialization

```java
BloomFilter bloomFilter = new BloomFilter(expectedMaxElements, expectedFalsePositiveProbability);
```

---

# Quadtree Implementation

A **Quadtree** is a tree data structure used to partition a **2D space** by recursively dividing it into **four equal quadrants**.

## How it Works
- Start with a single square region (root).
- When a region exceeds a set capacity, it is subdivided into:
  - Top-Left
  - Top-Right
  - Bottom-Left
  - Bottom-Right
- The process continues recursively.

## Why Use a Quadtree
- Efficient spatial partitioning
- Faster range and collision queries
- Reduces unnecessary checks in large 2D spaces

## Operations
- `insert(point)`
- `query(range)`
- `subdivide()`

## Time Complexity
- Average: **O(log n)**
- Worst case: **O(n)** (highly clustered data)

## Use Cases
- Game collision detection
- Maps and GIS systems
- Image processing
- Physics simulations
