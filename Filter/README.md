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
